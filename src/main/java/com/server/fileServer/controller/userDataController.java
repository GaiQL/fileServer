package com.server.fileServer.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.data.base.User;

@RestController
public class userDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
	
    @GetMapping("saveUserSql")
    public String saveUserSql(){
        String sql = "INSERT INTO USER ( userName,passWord ) VALUES ('zzzz','666')";
        int rows= jdbcTemplate.update(sql);
        return "执行成功，影响"+rows+"行";
    }
    
    //  获取版本；
    @PostMapping(path = "/sadddd")
	public static String sadddd(@RequestBody Map<String, String> params) throws Exception {
    	
    	return "123";
        
    }
    
    @GetMapping("getUserById")
    public User getUserById( Integer id ){
        String sql = "SELECT * FROM USER WHERE ID = ?";
        User user= jdbcTemplate.queryForObject( sql,new User(),new Object[]{id} );
        return user;
    }
    
    @GetMapping("getUserList")
    public List getUserList( int currentPage,int pageLimit ){
        String sql = "SELECT * FROM USER LIMIT " + ( currentPage - 1 )*pageLimit + "," + pageLimit;
        //List<User> list= jdbcTemplate.query(sql,new Object[]{userName}, new BeanPropertyRowMapper(User.class));
        List<User> list= jdbcTemplate.query(sql,new User());
        return list;
    }
    
    @GetMapping("getMapById")
    public Map getMapById(Integer id){
        String sql = "SELECT * FROM USER WHERE ID = ?";
        Map map= jdbcTemplate.queryForMap(sql,id);
        return map;
    }
    
    //http://localhost:8888/updateUserPassword?id=1&passWord=111
    @GetMapping("updateUserPassword")
    public String updateUserPassword(int id,String passWord){
        int rows= jdbcTemplate.update("UPDATE USER WHERE ID = ? SET PASSWORD = ?",id,passWord);
        return "执行成功，影响"+rows+"行";
    }
    
    //http://localhost:8888/deleteUserById?id=1
    @GetMapping("deleteUserById")
    public String deleteUserById(int id){
        int rows= jdbcTemplate.update("DELETE FROM  USER  WHERE ID = ?",id);
        return "执行成功，影响"+rows+"行";
    }
}