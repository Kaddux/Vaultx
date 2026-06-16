package com.pm.userservice.model;

public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false,name = "username")
    private String name;

    @Column(nullable = false,name = "password")
    private String password;

    @Email
    @Column(unique = true,nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "registered_date")
    private LocalDate registered_date;
}
