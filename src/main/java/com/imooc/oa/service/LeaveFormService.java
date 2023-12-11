package com.imooc.oa.service;

import com.imooc.oa.entity.Employee;
import com.imooc.oa.entity.LeaveForm;
import com.imooc.oa.entity.Notice;
import com.imooc.oa.entity.ProcessFlow;
import com.imooc.oa.mapper.EmployeeMapper;
import com.imooc.oa.mapper.LeaveFormMapper;
import com.imooc.oa.mapper.NoticeMapper;
import com.imooc.oa.mapper.ProcessFlowMapper;
import com.imooc.oa.service.exception.LeaveFormException;
import com.imooc.oa.utils.MybatisUtils;

import javax.servlet.annotation.WebServlet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/leave")
public class LeaveFormService {
    private EmployeeService employeeService = new EmployeeService();

    public LeaveForm createLeaveForm(LeaveForm form) {
        LeaveForm f = (LeaveForm) MybatisUtils.executeUpdate(sqlSession -> {
            // 1. level < 8 -> processing level >= 8 -> approved
            EmployeeMapper employeeMapper = sqlSession.getMapper(EmployeeMapper.class);
            Employee employee = employeeMapper.selectById(form.getEmployeeId());
            if (employee.getLevel() == 8) {
                form.setState("approved");
            } else {
                form.setState("processing");
            }
            LeaveFormMapper leaveFormMapper = sqlSession.getMapper(LeaveFormMapper.class);
            leaveFormMapper.insert(form);
            NoticeMapper noticeMapper = sqlSession.getMapper(NoticeMapper.class);
            // 2. insert into flow. set status as complete
            ProcessFlowMapper processFlowMapper = sqlSession.getMapper(ProcessFlowMapper.class);
            ProcessFlow processFlow = new ProcessFlow();
            processFlow.setFormId(form.getFormId());
            processFlow.setOperatorId(employee.getEmployeeId());
            processFlow.setAction("apply");
            processFlow.setCreateTime(new Date());
            processFlow.setOrderNo(1);
            processFlow.setState("complete");
            processFlow.setIsLast(0);
            processFlowMapper.insert(processFlow);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
            // 3.
            // 3.1. level < 7 manager if duration >= 72 -> CEO approve
            if (employee.getLevel() < 7) {
                Employee dmanager = employeeService.selectLeader(employee.getEmployeeId());
                ProcessFlow flow2 = new ProcessFlow();
                flow2.setFormId(form.getFormId());
                flow2.setOperatorId(dmanager.getEmployeeId());
                flow2.setAction("audit");
                flow2.setCreateTime(new Date());
                flow2.setState("process");
                flow2.setOrderNo(2);
                long diff = form.getEndTime().getTime() - form.getStartTime().getTime();
                float hours = diff / (1000 * 60 * 60);
                if (hours >= 72) {
                    flow2.setIsLast(0);
                    processFlowMapper.insert(flow2);
                    Employee manager = employeeService.selectLeader(dmanager.getEmployeeId());
                    ProcessFlow flow3 = new ProcessFlow();
                    flow3.setFormId(form.getFormId());
                    flow3.setOperatorId(manager.getEmployeeId());
                    flow3.setAction("audit");
                    flow3.setCreateTime(new Date());
                    flow3.setState("ready");
                    flow3.setOrderNo(3);
                    flow3.setIsLast(1);
                    processFlowMapper.insert(flow3);
                } else {
                    flow2.setIsLast(1);
                    processFlowMapper.insert(flow2);
                }
                String notice1 = String.format("{%s-%s} has been submitted. Waiting for approval!", sdf.format(form.getStartTime()), sdf.format(form.getEndTime()));
                noticeMapper.insert(new Notice(employee.getEmployeeId(), notice1));
                String notice2 = String.format("%s-%s has applied {%s- %s}, please approve asap", employee.getTitle(), employee.getName(), sdf.format(form.getStartTime()), sdf.format(form.getEndTime()));
                noticeMapper.insert(new Notice(dmanager.getDepartmentId(), notice2));
            }
            // 3.1 level = 7 CEO approve
            else if (employee.getLevel() == 7) {
                Employee manager = employeeService.selectLeader(employee.getEmployeeId());
                ProcessFlow flow2 = new ProcessFlow();
                flow2.setFormId(form.getFormId());
                flow2.setOperatorId(manager.getEmployeeId());
                flow2.setAction("audit");
                flow2.setCreateTime(new Date());
                flow2.setState("process");
                flow2.setOrderNo(2);
                flow2.setIsLast(1);
                processFlowMapper.insert(flow2);
                String notice1 = String.format("Your apply {%s-%s} has been submitted. Waiting for approval!", sdf.format(form.getStartTime()), sdf.format(form.getEndTime()));
                noticeMapper.insert(new Notice(employee.getEmployeeId(), notice1));
                String notice2 = String.format("%s-%s has applied {%s- %s}, please approve asap", employee.getTitle(), employee.getName(), sdf.format(form.getStartTime()), sdf.format(form.getEndTime()));
                noticeMapper.insert(new Notice(manager.getDepartmentId(), notice2));
            }
            // 3.3 level = 8 auto approve
            else if (employee.getLevel() == 8) {
                ProcessFlow flow2 = new ProcessFlow();
                flow2.setFormId(form.getFormId());
                flow2.setOperatorId(employee.getEmployeeId());
                flow2.setAction("audit");
                flow2.setResult("approved");
                flow2.setReason("Automatically approved");
                flow2.setCreateTime(new Date());
                flow2.setAuditTime(new Date());
                flow2.setState("complete");
                flow2.setOrderNo(2);
                flow2.setIsLast(1);
                processFlowMapper.insert(flow2);
                String notice1 = String.format("Your apply {%s-%s} has been automatically approved!", sdf.format(form.getStartTime()), sdf.format(form.getEndTime()));
                noticeMapper.insert(new Notice(employee.getEmployeeId(), notice1));
            }
            return form;
        });
        return f;
    }

    public List<Map> getLeaveFormList(String pfState, Long operatorId) {
        return (List<Map>) MybatisUtils.executeQuery(sqlSession -> {
            LeaveFormMapper mapper = sqlSession.getMapper(LeaveFormMapper.class);
            List<Map> maps = mapper.selectByParams(pfState, operatorId);
            return maps;
        });
    }

    public void audit(Long formId, Long operatorId, String result, String reason) {
        MybatisUtils.executeUpdate(sqlSession -> {
            ProcessFlowMapper processFlowMapper = sqlSession.getMapper(ProcessFlowMapper.class);
            List<ProcessFlow> flowList = processFlowMapper.selectByFormId(formId);
            if (flowList.size() == 0) {
                throw new LeaveFormException("Invalid audit process!");
            }
            // access ProcessFlow
            List<ProcessFlow> processList = flowList.stream().filter(p -> p.getOperatorId() == operatorId && p.getState().equals("process")).collect(Collectors.toList());
            ProcessFlow process = null;
            if (processList.size() == 0) {
                throw new LeaveFormException("No waiting node found!");
            } else {
                process = processList.get(0);
                process.setState("complete");
                process.setResult(result);
                process.setReason(reason);
                process.setAuditTime(new Date());
                processFlowMapper.update(process);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
            LeaveFormMapper leaveFormMapper = sqlSession.getMapper(LeaveFormMapper.class);
            LeaveForm form = leaveFormMapper.selectById(formId);
            NoticeMapper noticeMapper = sqlSession.getMapper(NoticeMapper.class);
            Employee operator = employeeService.selectById(operatorId);
            Employee employee = employeeService.selectById(form.getEmployeeId());
            if (process.getIsLast() == 1) {
                form.setState(result);
                leaveFormMapper.update(form);
                String notice1 = String.format("您的请假申请[%s-%s]%s%s已%s,审批意见:%s,审批流程已结束"
                        ,sdf.format(form.getStartTime()),sdf.format(form.getEndTime())
                        ,operator.getTitle(),operator.getName() , result, reason);
                noticeMapper.insert(new Notice(form.getEmployeeId(), notice1));

                String notice2 = String.format("%s-%s提起请假申请[%s-%s]您已%s,审批意见:%s,审批流程已结束" ,
                        employee.getTitle() , employee.getName() , sdf.format( form.getStartTime()) , sdf.format(form.getEndTime()),
                        result , reason);//发给审批人的通知
                noticeMapper.insert(new Notice(operator.getEmployeeId(),notice2));
            } else {
                // not the last node & approved, CEO ready -> process
                List<ProcessFlow> readyList = flowList.stream().filter(p -> p.getState().equals("ready")).collect(Collectors.toList());
                if (result.equals("approved")) {
                    ProcessFlow readyProcess = readyList.get(0);
                    readyProcess.setState("process");
                    processFlowMapper.update(readyProcess);
                    //消息1: 通知表单提交人,部门经理已经审批通过,交由上级继续审批
                    String notice1 = String.format("您的请假申请[%s-%s]%s%s已批准,审批意见:%s ,请继续等待上级审批" ,
                            sdf.format(form.getStartTime()) , sdf.format(form.getEndTime()),
                            operator.getTitle() , operator.getName(),reason);
                    noticeMapper.insert(new Notice(form.getEmployeeId(),notice1));

                    //消息2: 通知总经理有新的审批任务
                    String notice2 = String.format("%s-%s提起请假申请[%s-%s],请尽快审批" ,
                            employee.getTitle() , employee.getName() , sdf.format( form.getStartTime()) , sdf.format(form.getEndTime()));
                    noticeMapper.insert(new Notice(readyProcess.getOperatorId(),notice2));

                    //消息3: 通知部门经理(当前经办人),员工的申请单你已批准,交由上级继续审批
                    String notice3 = String.format("%s-%s提起请假申请[%s-%s]您已批准,审批意见:%s,申请转至上级领导继续审批" ,
                            employee.getTitle() , employee.getName() , sdf.format( form.getStartTime()) , sdf.format(form.getEndTime()), reason);
                    noticeMapper.insert(new Notice(operator.getEmployeeId(),notice3));

                } else if (result.equals("refused")) {
                    for (ProcessFlow p : readyList) {
                        p.setState("cancel");
                        processFlowMapper.update(p);
                    }
                    form.setState("refused");
                    leaveFormMapper.update(form);
                    //消息1: 通知申请人表单已被驳回
                    String notice1 = String.format("您的请假申请[%s-%s]%s%s已驳回,审批意见:%s,审批流程已结束" ,
                            sdf.format(form.getStartTime()) , sdf.format(form.getEndTime()),
                            operator.getTitle() , operator.getName(),reason);
                    noticeMapper.insert(new Notice(form.getEmployeeId(),notice1));

                    //消息2: 通知经办人表单"您已驳回"
                    String notice2 = String.format("%s-%s提起请假申请[%s-%s]您已驳回,审批意见:%s,审批流程已结束" ,
                            employee.getTitle() , employee.getName() , sdf.format( form.getStartTime()) , sdf.format(form.getEndTime()), reason);
                    noticeMapper.insert(new Notice(operator.getEmployeeId(),notice2));
                }
                // rejected, CEO ready -> cancel

            }
            return null;
        });
    }
}
