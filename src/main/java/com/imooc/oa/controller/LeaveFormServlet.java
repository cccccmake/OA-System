package com.imooc.oa.controller;

import com.imooc.oa.entity.LeaveForm;
import com.imooc.oa.service.LeaveFormService;
import com.imooc.oa.utils.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@WebServlet("/api/leave/*")
public class LeaveFormServlet extends HttpServlet {
    private LeaveFormService leaveFormService = new LeaveFormService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=utf-8");
        String uri = req.getRequestURI();
        String methodName = uri.substring(uri.lastIndexOf("/") + 1);
        if (methodName.equals("create")) {
            this.create(req, resp);
        } else if (methodName.equals("list")) {
            this.list(req, resp);
        } else if (methodName.equals("audit")) {
            this.audit(req, resp);
        }
    }

    private void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String strEmployeeId = req.getParameter("eid");
        String formType = req.getParameter("formType");
        String startTime = req.getParameter("startTime");
        String endTime = req.getParameter("endTime");
        String reason = req.getParameter("reason");
        LeaveForm form = new LeaveForm();
        form.setEmployeeId(Long.parseLong(strEmployeeId));
        form.setStartTime(new Date(Long.parseLong(startTime)));
        form.setEndTime(new Date(Long.parseLong(endTime)));
        form.setReason(reason);
        form.setCreateTime(new Date());
        form.setFormType(Integer.parseInt(formType));
        ResponseUtils responseUtils = null;
        try {
            leaveFormService.createLeaveForm(form);
            responseUtils = new ResponseUtils();
        } catch (Exception e) {
            e.printStackTrace();
            responseUtils = new ResponseUtils(e.getClass().getSimpleName(), e.getMessage());
        }
        resp.getWriter().println(responseUtils.toJsonString());
    }

    public void list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String employeeId = req.getParameter("eid");
        ResponseUtils response = null;
        try {
            List<Map> formList = leaveFormService.getLeaveFormList("process", Long.parseLong(employeeId));
            response = new ResponseUtils().put("list", formList);
        }catch (Exception e){
            e.printStackTrace();
            response = new ResponseUtils(e.getClass().getSimpleName(), e.getMessage());
        }
        resp.getWriter().println(response.toJsonString());
    }

    public void audit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String formId = req.getParameter("formId");
        String result = req.getParameter("result");
        String reason = req.getParameter("reason");
        String eid = req.getParameter("eid");
        ResponseUtils response = null;
        try {
            leaveFormService.audit(Long.parseLong(formId), Long.parseLong(eid), result, reason);
            response = new ResponseUtils();
        }catch(Exception e){
            e.printStackTrace();
            response = new ResponseUtils(e.getClass().getSimpleName(), e.getMessage());
        }
        resp.getWriter().println(response.toJsonString());

    }
}
