# Java-Custom-ORM
The following project is a simple and customizable Object-Relational Mapping framework for Java.
This project aims to provide basic ORM functionalities such as insert, update, retrieve, and delete operations, as well as the ability to create and manage database tables dynamically. Built using Maven, this project demonstrates how to map Java objects to database tables through reflection and annotations, offering a hands-on approach to understanding ORM principles.

## Features:
- **Entities**: Java classes that represent database tables, with automatic mapping of fields to columns.
- **CRUD Operations**: Basic functionality to insert, update, and retrieve records from the database.
- **Dynamic Table Creation**: Automatically creates tables if they don’t already exist.
- **Database Connectivity**: Easily connects to your database using a configuration-based connection.
- **Annotations**: Uses custom annotations (`@Entity`, `@Column`, `@Id`) for class-to-table and field-to-column mappings.
- **Reflection**: Utilizes Java reflection to handle entity data dynamically, without requiring manual queries for each entity type.

## CRUD Operations:
- **Insert**: Adds a new entity to the database. If the entity already exists, it performs an update.
- **Update**: Modifies the properties of an existing entity in the database.
- **Retrieve**: Retrieves entities from the database either by criteria or all entries.
- **Delete**: Deletes entities from the database based on specific criteria.

## Example Usage:
1. **Create an Entity**: 
   - Define a class with fields and annotations to represent a table.
2. **Persist Entities**: 
   - Use the `EntityManager` to save entities to the database, whether inserting new records or updating existing ones.
3. **Retrieve Entities**: 
   - Use `find()` or `findFirst()` methods to fetch data from the database.
