package com.example.demo.model;


public class Student {
    private String name;
    private int id;
    private String gender;
    public Student(){}
    public Student(String name,int id,String gender){
        this.name = name;
        this.id = id;
        this.gender = gender;
    }
    public String getName(){
        return name;
    }
    public int getId(){
        return id;
    }
    public String getGender(){
        return gender;
    }
}