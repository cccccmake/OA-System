package com.imooc.oa.controller;

import com.imooc.oa.entity.Notice;
import com.imooc.oa.mapper.NoticeMapper;
import com.imooc.oa.service.NoticeService;
import com.imooc.oa.utils.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/notice/list")
public class NoticeServlet extends HttpServlet {
    private NoticeService noticeService = new NoticeService();
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String employeeId = req.getParameter("eid");
        resp.setContentType("application/json;charset=utf-8");
        ResponseUtils response = null;
        try {
            List<Notice> noticeList = noticeService.getNoticeList(Long.parseLong(employeeId));
            response = new ResponseUtils().put("list", noticeList);
        }catch(Exception e){
            e.printStackTrace();
            response = new ResponseUtils(e.getClass().getSimpleName(), e.getMessage());
        }
        resp.getWriter().println(response.toJsonString());
    }
}
