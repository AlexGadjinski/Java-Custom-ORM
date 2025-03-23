package org.example.orm.context;

import org.example.orm.core.Column;
import org.example.orm.core.Entity;
import org.example.orm.core.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityManager<E> implements DbContext<E> {
    private static final String INSERT_SQL = "INSERT INTO %s(%s) VALUES (%s)";
    private static final String UPDATE_SQL = "UPDATE %s SET %s WHERE id = %d";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE %s(%s)";
    private static final String CHECK_TABLE_SQL = "SELECT * FROM information_schema.columns " +
            "WHERE table_schema = 'mini_orm' AND table_name = ?";
    private static final String ALTER_TABLE_SQL = "ALTER TABLE %s %s";
    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s";
    private static final String DELETE_ENTITY_SQL = "DELETE FROM %s WHERE id = %s";
    private final Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void doCreate(Class<E> entityClass) throws SQLException {
        String tableName = getTableName(entityClass);

        if (tableExists(tableName)) {
            System.out.println("Table " + tableName + " already exists");
            return;
        }

        String columnDefinitions = getColumnDefinitions(entityClass);

        String query = CREATE_TABLE_SQL.formatted(tableName, columnDefinitions);
        connection.prepareStatement(query).execute();
    }

    private boolean tableExists(String tableName) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(CHECK_TABLE_SQL);
        preparedStatement.setString(1, tableName);
        ResultSet resultSet = preparedStatement.executeQuery();
        return resultSet.next();
    }

    private String getColumnDefinitions(Class<E> entityClass) {
        List<String> columnDefinitions = new ArrayList<>();
        Field[] fields = entityClass.getDeclaredFields();

        List<String> columnNames = new ArrayList<>();
        for (Field f : fields) {
            String columnName = f.isAnnotationPresent(Column.class)
                    ? f.getAnnotation(Column.class).name()
                    : "id";
            columnNames.add(columnName);
        }
        List<String> columnTypes = Arrays.stream(fields).map(this::getColumnType).toList();

        for (int i = 0; i < columnNames.size(); i++) {
            String columnDefinition = String.format("%s %s", columnNames.get(i), columnTypes.get(i));
            if (columnNames.get(i).equals("id")) {
                columnDefinition += " PRIMARY KEY AUTO_INCREMENT";
            }
            columnDefinitions.add(columnDefinition);
        }

        return String.join(", ", columnDefinitions);
    }

    private String getColumnType(Field f) {
        return switch (f.getType().getSimpleName()) {
            case "int", "Integer" -> "INT";
            case "String" -> "VARCHAR(255)";
            case "LocalDate" -> "DATE";
            case "double", "Double" -> "DOUBLE(8, 2)";
            default -> throw new IllegalArgumentException("Unsupported type " + f.getType());
        };
    }

    // Alter will only add new columns to the table
    @Override
    public void doAlter(Class<E> entityClass) throws SQLException {
        String tableName = getTableName(entityClass);

        String newDefinitions = getAlterDefinitions(entityClass, tableName);

        if (!newDefinitions.equals("")) {
            String query = ALTER_TABLE_SQL.formatted(tableName, newDefinitions);
            connection.prepareStatement(query).execute();
        }
    }

    private String getAlterDefinitions(Class<E> entityClass, String tableName) throws SQLException {
        List<String> columnNames = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotation(Column.class).name()).collect(Collectors.toList());

        String query = "SELECT * FROM " + tableName;
        ResultSet resultSet = connection.createStatement().executeQuery(query);
        resultSet.next();

        columnNames = columnNames.stream()
                .filter(columnName -> {
                    try {
                        resultSet.getString(columnName);
                        return false;
                    } catch (SQLException e) {
                        return true;
                    }
                }).toList();

        if (columnNames.isEmpty()) {
            return "";
        }

        List<String> alterDefinitions = new ArrayList<>();
        columnNames.forEach(c -> alterDefinitions.add(getAlterDefinition(entityClass, c)));
        return String.join(", ", alterDefinitions);
    }

    private String getAlterDefinition(Class<E> entityClass, String columnName) {
        Field field = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> f.getAnnotation(Column.class).name().equals(columnName))
                .toList().get(0);

        return "ADD COLUMN %s %s".formatted(columnName, getColumnType(field));
    }


    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        int idValue = getIdValue(entity);
        if (idValue == 0) {
            return doInsert(entity);
        }
        return doUpdate(entity, idValue);
    }

    // Find table name
    // Find column names
    // Find column values
    // Generate + execute sql
    private boolean doInsert(E entity) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        List<String> columnNames = findEntityColumns(entity);
        List<String> columnValues = findEntityValues(entity);

        String query = String.format(INSERT_SQL,
                tableName,
                String.join(", ", columnNames),
                String.join(", ", columnValues));

        int rowsInserted = connection.prepareStatement(query).executeUpdate();
        return rowsInserted == 1;
    }

    private boolean doUpdate(E entity, int idValue) throws IllegalAccessException, SQLException {
        String tableName = getTableName(entity.getClass());
        List<String> columnNames = findEntityColumns(entity);
        List<String> columnValues = findEntityValues(entity);

        List<String> updateColumns = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            updateColumns.add(String.format("%s = %s",
                    columnNames.get(i),
                    columnValues.get(i)));
        }

        String query = UPDATE_SQL.formatted(
                tableName,
                String.join(", ", updateColumns),
                idValue);

        int rowsUpdated = connection.prepareStatement(query).executeUpdate();
        return rowsUpdated == 1;
    }

    private List<String> findEntityValues(E entity) throws IllegalAccessException {
        List<String> result = new ArrayList<>();
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Column.class)) {
                f.setAccessible(true);
                result.add("'" + f.get(entity).toString() + "'");
            }
        }
        return result;
    }

    // FIXME: Quotes around column names (because column names may clash with keywords in MySQL)
    private List<String> findEntityColumns(E entity) {
        return Arrays.stream(entity.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotation(Column.class).name())
                .toList();
    }

    @Override
    public Iterable<E> find(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return find(table, null);
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String tableName = getTableName(table);
        String query = String.format("SELECT * FROM %s %s",
                tableName,
                where == null ? "" : where);

        ResultSet resultSet = connection.prepareStatement(query).executeQuery();

        List<E> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(mapEntity(table, resultSet));
        }

        return result;
    }

    @Override
    public E findFirst(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return findFirst(table, null);
    }

    @Override
    public E findFirst(Class<E> table, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String withLimit = where == null ? "LIMIT 1" : where + " LIMIT 1";
        Iterable<E> es = find(table, where);

        if (!es.iterator().hasNext()) {
            return null;
        }
        return es.iterator().next();

//        String tableName = getTableName(table);
//        String query = String.format("SELECT * FROM %s %s %s",
//                tableName,
//                where == null ? "" : where,
//                "LIMIT 1");
//
//        ResultSet resultSet = connection.prepareStatement(query).executeQuery();
//        if (resultSet.next()) {
//            // Map to E
//            return mapEntity(table, resultSet);
//        }
//
//        return null;
    }

    private E mapEntity(Class<E> type, ResultSet dbResult) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        // създаваме си нова инстанция, след което попълваме полетата едно по едно
        E result = type.getDeclaredConstructor().newInstance();

        for (Field f : type.getDeclaredFields()) {
            if (f.isAnnotationPresent(Column.class) || f.isAnnotationPresent(Id.class)) {
                result = mapField(result, f, dbResult);
            }
        }
        return result;
    }

    /**
     * по принцип тук го модифицираме in place и може да е void,
     * но е по-добра практика да го връщаме като стойност все пак
     **/
    private E mapField(E object, Field f, ResultSet dbResult) throws IllegalAccessException, SQLException {
        String columnName = f.isAnnotationPresent(Id.class) ? "id" : f.getAnnotation(Column.class).name();
        Object dbValue = mapValue(f, columnName, dbResult);

        f.setAccessible(true);
        f.set(object, dbValue);

        return object;
    }

    private Object mapValue(Field f, String columnName, ResultSet dbResult) throws SQLException {
        if (f.getType() == int.class || f.getType() == Integer.class) {
            return dbResult.getInt(columnName);
        } else if (f.getType() == String.class) {
            return dbResult.getString(columnName);
        } else if (f.getType() == LocalDate.class) {
            String date = dbResult.getString(columnName);
            return LocalDate.parse(date);
        } else if (f.getType() == Double.class) {
            return dbResult.getDouble(columnName);
        }
        throw new IllegalArgumentException("Unsupported type " + f.getType());
    }

    @Override
    public int doDelete(Class<E> table, String where) throws SQLException {
        String query = DELETE_SQL.formatted(getTableName(table), where);

        return connection.createStatement().executeUpdate(query);
    }

    @Override
    public int doDelete(E entity) throws SQLException, NoSuchFieldException, IllegalAccessException {
        String tableName = getTableName(entity.getClass());

        int idValue = getIdValue(entity);

        String query = DELETE_ENTITY_SQL.formatted(tableName, idValue);
        return connection.createStatement().executeUpdate(query);
    }

    private String getTableName(Class<?> clazz) {
        Entity annotation = clazz.getAnnotation(Entity.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Entity annotation missing!");
        }
        return annotation.name();
    }

    // Get all fields
    // Find field with @Id
    // Get value from field
    private int getIdValue(E entity) throws IllegalAccessException {
        List<Field> idFields = Arrays.stream(entity.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class)).toList();
        if (idFields.size() != 1) {
            throw new IllegalArgumentException("Entity must have 1 ID field");
        }
        Field idField = idFields.get(0);
        idField.setAccessible(true);
        return (int) idField.get(entity);
    }
}
