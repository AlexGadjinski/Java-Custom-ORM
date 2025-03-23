package org.example;

import org.example.entities.User;
import org.example.orm.config.MyConnector;
import org.example.orm.context.EntityManager;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        MyConnector.createConnection("root", "1234", "mini_orm");
        Connection connection = MyConnector.getConnection();

        EntityManager<User> em = new EntityManager<>(connection);
//        em.doCreate(User.class);
        em.doAlter(User.class);

//        User user1 = new User("user", "pass", 20, LocalDate.now());
//        User user2 = new User("user2", "pass2", 22, LocalDate.now());
//        user2.setId(2);
//        user2.setSalary(1500.00);
//        user2.setPhoneNumber("+359882111");

//        em.persist(user2);

        User firstUser = em.findFirst(User.class);
//
//        firstUser.setUsername("updated");
//        em.persist(firstUser);
//
//        System.out.println();
//
//        Iterable<User> users = em.find(User.class);
//        users.forEach(u -> System.out.println(u.toString()));
//
//        System.out.println();

        em.doDelete(User.class, "id = 1");
//        em.doDelete(firstUser);
    }
}
