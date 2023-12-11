package com.imooc.oa.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.oa.entity.User;
import com.imooc.oa.service.UserService;
import com.imooc.oa.utils.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    private UserService userService = new UserService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        // processing username & password, therefore, application/json
        resp.setContentType("application/json;charset=utf-8");
        // 1. get user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // 2. invoke logic
        ResponseUtils responseUtils = null;
        try {
            User user = userService.checkLogin(username, password);
            // remove sensitive info
            user.setPassword(null);
            user.setSalt(null);
            // process result, 0 indicates success
            // Map data = new LinkedHashMap();
            responseUtils = new ResponseUtils().put("user", user);
        }catch (Exception e){
            e.printStackTrace();
            responseUtils = new ResponseUtils(e.getClass().getSimpleName(), e.getMessage());
        }
        // 3. return json result
        resp.getWriter().println(responseUtils.toJsonString());
    }
}
