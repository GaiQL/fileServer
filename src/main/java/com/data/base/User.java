package com.data.base;


import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User implements RowMapper<User> {
    private int id;
    private String userName;
    private String passWord;
    private int age;
	private int status = 1;

//    public User(int id, String user_name, String pass_word) {
//        this.id = id;
//        this.user_name = user_name;
//        this.pass_word = pass_word;
//    }

    public int getId() {
        return id;
    }
    public int getStatus() {
        return status;
    }
    public String getUserName() {
        return userName;
    }
    public String getPass_word() {
        return passWord;
    }
    public int getAgeHHHH() {
        return age;
    }

    
    public void setId(int id) {
        this.id = id;
    }
    public void setUserName(String user_name) {
        this.userName = user_name;
    }
    public void setPass_word(String pass_word) {
        this.passWord = pass_word;
    }
    public void setAgeHHH(int agezzz) {
        this.age = agezzz;
    }
    public void iniData() {
    	this.status = 1;
    }

    @Override
    public User mapRow(ResultSet resultSet, int i) throws SQLException {
        User user = new User();
        user.iniData();
        user.setId(resultSet.getInt("id"));
        user.setAgeHHH(resultSet.getInt("age"));
        user.setUserName(resultSet.getString("userName"));
        user.setPass_word(resultSet.getString("passWord"));
        return user;
    }
}