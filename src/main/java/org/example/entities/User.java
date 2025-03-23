package org.example.entities;

import org.example.orm.core.Column;
import org.example.orm.core.Entity;
import org.example.orm.core.Id;

import java.time.LocalDate;

@Entity(name = "users")
public class User {
    @Id
    private int id;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "age")
    private int age;
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "salary")
    private Double salary;

    public User() {}
    public User(String username, String password, int age, LocalDate registrationDate) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.registrationDate = registrationDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", registrationDate=" + registrationDate +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", salary=" + salary +
                '}';
    }
}
