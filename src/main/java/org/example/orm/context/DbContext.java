package org.example.orm.context;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public interface DbContext<E> {
    void doCreate(Class<E> entityClass) throws SQLException; // Create table ...
    void doAlter(Class<E> entityClass) throws SQLException; // Alter
    boolean persist(E entity) throws IllegalAccessException, SQLException; // Insert | Update
    Iterable<E> find(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException; // Select
    Iterable<E> find(Class<E> table, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException; // Select ... where ...
    E findFirst(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException; // Select ... limit 1
    E findFirst(Class<E> table, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException; // Select ... where ... limit 1
    int doDelete(Class<E> table, String where) throws SQLException;
    int doDelete(E entity) throws SQLException, NoSuchFieldException, IllegalAccessException;
}
