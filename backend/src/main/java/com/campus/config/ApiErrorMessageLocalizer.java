package com.campus.config;

import java.util.Map;
import java.util.regex.Pattern;

public final class ApiErrorMessageLocalizer {

    private static final String DEFAULT_ERROR_MESSAGE = "请求失败，请稍后重试";
    private static final Pattern ASCII_LETTER_PATTERN = Pattern.compile("[A-Za-z]");
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\p{IsHan}]");

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("account is banned", "账号已被封禁"),
            Map.entry("unauthorized", "请先登录"),
            Map.entry("forbidden", "没有权限执行该操作"),
            Map.entry("not found", "请求的资源不存在"),
            Map.entry("invalid request", "请求参数无效"),
            Map.entry("internal server error", "服务器开小差了，请稍后再试"),
            Map.entry("request failed", "请求失败，请稍后重试"),
            Map.entry("user not found", "用户不存在"),
            Map.entry("community post not found", "帖子不存在"),
            Map.entry("community comment not found", "评论不存在"),
            Map.entry("invalid community tag", "社区标签无效"),
            Map.entry("invalid community hot period", "社区热榜周期无效"),
            Map.entry("invalid community hot limit", "社区热榜数量无效"),
            Map.entry("invalid favorite type", "收藏类型无效"),
            Map.entry("cannot reply to a reply", "暂不支持回复二级评论"),
            Map.entry("invalid experience target label", "经历目标标签过长"),
            Map.entry("invalid experience outcome label", "经历结果标签过长"),
            Map.entry("invalid experience timeline summary", "经历时间线摘要过长"),
            Map.entry("invalid experience action summary", "经历行动摘要过长"),
            Map.entry("job not found", "岗位不存在"),
            Map.entry("invalid job type", "岗位类型无效"),
            Map.entry("invalid education requirement", "学历要求无效"),
            Map.entry("invalid source url", "来源链接无效"),
            Map.entry("invalid deadline format", "截止时间格式无效"),
            Map.entry("deleted job cannot be updated", "已删除岗位不能编辑"),
            Map.entry("deleted job cannot be published", "已删除岗位不能发布"),
            Map.entry("deleted job cannot be offlined", "已删除岗位不能下线"),
            Map.entry("only published job can be offlined", "只有已发布岗位可以下线"),
            Map.entry("job is not ready for publish", "岗位信息未完善，暂不能发布"),
            Map.entry("job import validation failed", "岗位导入校验失败"),
            Map.entry("job import file unavailable", "岗位导入文件暂不可用"),
            Map.entry("job import only supports csv files", "岗位导入仅支持 CSV 文件"),
            Map.entry("csv file name is required", "请提供 CSV 文件名"),
            Map.entry("csv file is empty", "CSV 文件为空"),
            Map.entry("csv file must be utf-8 encoded", "CSV 文件必须使用 UTF-8 编码"),
            Map.entry("job import row limit exceeded", "岗位导入行数超过限制"),
            Map.entry("missing required header", "缺少必填表头"),
            Map.entry("duplicate header", "表头重复"),
            Map.entry("unsupported header", "存在不支持的表头"),
            Map.entry("duplicate source url in file", "文件内来源链接重复"),
            Map.entry("duplicate source url already exists", "来源链接已存在"),
            Map.entry("job sync unavailable", "岗位同步服务暂不可用"),
            Map.entry("job sync request failed", "岗位同步请求失败"),
            Map.entry("invalid job sync feed", "岗位同步数据源无效"),
            Map.entry("resume not found", "简历不存在"),
            Map.entry("resume is required", "请选择简历"),
            Map.entry("resume preview only supports pdf or docx", "简历预览仅支持 PDF 或 DOCX"),
            Map.entry("resume preview unavailable", "简历预览暂不可用"),
            Map.entry("resume file name is required", "请提供简历文件名"),
            Map.entry("unsupported resume file type", "暂不支持该简历文件类型"),
            Map.entry("resume file is too large", "简历文件过大"),
            Map.entry("failed to store resume file", "简历文件保存失败"),
            Map.entry("resource not found", "资源不存在"),
            Map.entry("invalid resource category", "资源分类无效"),
            Map.entry("category is required", "请选择资源分类"),
            Map.entry("resource preview only supports pdf, pptx or docx", "资源预览仅支持 PDF、PPTX 或 DOCX"),
            Map.entry("zip preview only supports zip resources", "压缩包预览仅支持 ZIP 资源"),
            Map.entry("resource file unavailable", "资源文件暂不可用"),
            Map.entry("resource preview unavailable", "资源预览暂不可用"),
            Map.entry("pptx preview unavailable", "PPTX 预览暂不可用"),
            Map.entry("zip preview unavailable", "压缩包预览暂不可用"),
            Map.entry("only rejected resource can be resubmitted", "只有审核未通过的资源可以重新提交"),
            Map.entry("rejected resource cannot be published", "已拒绝资源不能发布"),
            Map.entry("resource is not ready for publish", "资源文件未准备完成，暂不能发布"),
            Map.entry("only pending resource can be rejected", "只有待审核资源可以驳回"),
            Map.entry("reason is required when rejecting resource", "驳回资源时请填写原因"),
            Map.entry("only published resource can be offlined", "只有已发布资源可以下线"),
            Map.entry("notification not found", "通知不存在"),
            Map.entry("application not found", "申请记录不存在"),
            Map.entry("already applied to this job", "你已申请过该岗位"),
            Map.entry("failed to store application resume snapshot", "申请简历快照保存失败"),
            Map.entry("application resume preview only supports pdf or docx", "申请简历预览仅支持 PDF 或 DOCX"),
            Map.entry("application resume preview unavailable", "申请简历预览暂不可用"),
            Map.entry("file is required", "请先选择文件"),
            Map.entry("Required part 'file' is not present.", "请先选择文件"),
            Map.entry("Required part 'chunk' is not present.", "请上传分片文件"),
            Map.entry("file is too large", "文件过大"),
            Map.entry("file name is required", "请提供文件名"),
            Map.entry("unsupported file type", "暂不支持该文件类型"),
            Map.entry("failed to store resource file", "资料文件保存失败"),
            Map.entry("failed to store resource chunk", "资料分片保存失败"),
            Map.entry("failed to complete resource upload", "资料上传合并失败"),
            Map.entry("failed to create upload session", "上传会话创建失败"),
            Map.entry("upload session not found", "上传会话不存在或已过期"),
            Map.entry("invalid chunk index", "分片序号无效"),
            Map.entry("chunk is required", "请上传分片文件"),
            Map.entry("invalid chunk size", "分片大小不匹配"),
            Map.entry("chunk size is required", "请提供分片大小"),
            Map.entry("chunk size is too large", "分片大小超过限制"),
            Map.entry("resource chunks are incomplete", "资料分片尚未上传完整"),
            Map.entry("assembled resource size mismatch", "资料合并后大小不匹配"),
            Map.entry("invalid period", "统计周期无效"),
            Map.entry("invalid discover tab", "发现页标签无效"),
            Map.entry("invalid discover period", "发现页周期无效"),
            Map.entry("invalid discover limit", "发现页数量无效"),
            Map.entry("track is required", "请选择申请方向"),
            Map.entry("invalid track", "申请方向无效"),
            Map.entry("invalid anchorDate", "日期参数无效"),
            Map.entry("assessment questions not configured", "测评题目尚未配置"),
            Map.entry("incomplete answers", "请完成全部测评题目"),
            Map.entry("duplicate question answers", "同一题目不能重复作答"),
            Map.entry("unknown question", "测评题目不存在"),
            Map.entry("unknown option", "测评选项不存在"),
            Map.entry("option does not belong to question", "测评选项与题目不匹配"),
            Map.entry("invalid score result", "测评结果无效"),
            Map.entry("at least 2 schools required", "请至少选择 2 所学校"),
            Map.entry("at most 4 schools allowed", "最多只能选择 4 所学校"),
            Map.entry("duplicate school ids", "学校不能重复选择"),
            Map.entry("school not found", "学校不存在"),
            Map.entry("mixed school tracks", "请选择同一方向的学校进行对比"),
            Map.entry("search query is required", "请输入搜索关键词"),
            Map.entry("invalid search type", "搜索类型无效"),
            Map.entry("invalid search sort", "搜索排序无效"),
            Map.entry("phone already registered", "手机号已注册"),
            Map.entry("phone or verification code is incorrect", "手机号或验证码错误"),
            Map.entry("invalid purpose", "验证码用途无效"),
            Map.entry("verification code has expired", "验证码已过期"),
            Map.entry("verification application not found", "认证申请不存在"),
            Map.entry("verification application is not pending", "认证申请不在待审核状态"),
            Map.entry("reason is required when rejecting application", "驳回申请时请填写原因"),
            Map.entry("invalid review action", "审核操作无效"),
            Map.entry("only normal users can apply for verification", "只有普通用户可以申请认证"),
            Map.entry("user is already verified", "用户已完成认证"),
            Map.entry("pending verification application already exists", "已有待审核的认证申请"),
            Map.entry("admin account status cannot be changed", "不能修改管理员账号状态"),
            Map.entry("invalid limit", "数量参数无效"),
            Map.entry("invalid resource status", "资源状态无效"),
            Map.entry("invalid resource id", "资源 ID 无效"),
            Map.entry("minio migration unavailable", "资源迁移服务暂不可用"),
            Map.entry("minio preview migration unavailable", "资源预览迁移服务暂不可用"),
            Map.entry("value is required", "请填写必填项"),
            Map.entry("value exceeds max length", "内容长度超过限制"),
            Map.entry("must be 11 digits", "手机号必须为 11 位数字"),
            Map.entry("must be 6 digits", "验证码必须为 6 位数字"));

    private ApiErrorMessageLocalizer() {
    }

    public static String localize(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_ERROR_MESSAGE;
        }

        String normalizedMessage = message.trim();
        String localizedMessage = MESSAGES.get(normalizedMessage);
        if (localizedMessage != null) {
            return localizedMessage;
        }
        if (normalizedMessage.endsWith(" is required")) {
            return "请填写必填项";
        }
        if (containsCjk(normalizedMessage)) {
            return normalizedMessage;
        }
        if (containsAsciiLetter(normalizedMessage)) {
            return DEFAULT_ERROR_MESSAGE;
        }
        return normalizedMessage;
    }

    private static boolean containsCjk(String message) {
        return CJK_PATTERN.matcher(message).find();
    }

    private static boolean containsAsciiLetter(String message) {
        return ASCII_LETTER_PATTERN.matcher(message).find();
    }
}
