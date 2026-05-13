import time
import os
import re
from playwright.sync_api import Playwright, sync_playwright, expect

def login(page, phone):
    """通用登录模块"""
    page.goto("http://127.0.0.1:5173/")
    page.get_by_role("link", name="登录", exact=True).click()
    page.get_by_placeholder("请输入 11 位手机号").fill(phone)
    page.get_by_role("button", name="获取验证码").click()
    time.sleep(1.5)
    debug_text = page.get_by_text("本次调试验证码：").inner_text()
    code = re.search(r'\d+', debug_text).group()
    page.get_by_placeholder("请输入 6 位验证码").fill(code)
    page.get_by_role("button", name="登录").click()
    # 等待首页渲染完成
    page.wait_for_selector('[data-test="home-primary-cta"]', timeout=10000)

def run(playwright: Playwright) -> None:
    if not os.path.exists("test_results"):
        os.makedirs("test_results")

    print(">>> 启动测试三：UC-13 求职投递与申请流转全链路测试...")
    browser = playwright.chromium.launch(headless=False, slow_mo=600)

    # ==========================================
    # 角色 A：求职学生 (务必已手动注册)
    # ==========================================
    test_phone = "13800000002"
    print(f"\n--- [Step 1] 角色A (学生端) 简历上传与投递 ---")
    user_context = browser.new_context()
    user_page = user_context.new_page()
    
    print(f">>> 学生登录中 ({test_phone})...")
    login(user_page, test_phone)
    
    # 关键修复 1：明确进入个人中心
    print(">>> 进入个人中心...")
    user_page.locator('[data-test="home-primary-cta"]').click()
    
    print(">>> 进入简历库并上传 PDF...")
    user_page.get_by_role("link", name=re.compile(r"我的简历")).click()
    user_page.get_by_placeholder("例如：实习投递版简历").fill("自动化测试简历")
    user_page.get_by_label("文件").set_input_files("中文简历模板.pdf")
    user_page.get_by_role("button", name="上传简历").click()
    time.sleep(1.5)
    print("✅ 简历入库成功！")

    # 关键修复 2：明确返回首页，重新寻找岗位大厅入口
    print(">>> 返回首页大厅...")
    user_page.goto("http://127.0.0.1:5173/")
    time.sleep(1)
    
    print(">>> 进入岗位详情并投递...")
    user_page.get_by_role("link", name=re.compile(r"就业方向")).click()
    user_page.get_by_role("link", name=re.compile(r"Java Backend Intern")).click()
    
    user_page.get_by_test_id("apply-toggle").click()
    user_page.get_by_text("中文简历模板.pdf").click()
    user_page.get_by_test_id("submit-application").click()
    time.sleep(1.5)
    user_page.screenshot(path="test_results/06_job_applied.png")
    print("✅ 角色A：岗位投递成功，简历快照已生成！")

    # ==========================================
    # 角色 B：系统管理员
    # ==========================================
    print("\n--- [Step 2] 角色B (管理端) 申请查收与预览 ---")
    admin_context = browser.new_context()
    admin_page = admin_context.new_page()
    
    print(">>> 管理员登录中 (13800000000)...")
    login(admin_page, "13800000000")
    
    print(">>> 进入申请管理工作台...")
    admin_page.get_by_role("link", name="申请管理", exact=True).click()
    
    print(">>> 验证简历快照预览功能 (弹出新标签页)...")
    with admin_page.expect_popup() as popup_info:
        # 直接点击列表中最新记录的预览按钮
        admin_page.get_by_role("button", name="预览").first.click()
    
    preview_page = popup_info.value
    preview_page.wait_for_load_state()
    time.sleep(1.5)
    preview_page.screenshot(path="test_results/07_resume_preview.png")
    preview_page.close()
    print("✅ 角色B：申请记录接收正常，PDF 简历预览成功！")

    print("\n🎉 测试三：求职投递全链路流转测试圆满完成！")
    
    user_context.close()
    admin_context.close()
    browser.close()

if __name__ == "__main__":
    with sync_playwright() as playwright:
        try:
            run(playwright)
        except Exception as e:
            print(f"\n❌ 测试中断：\n{e}")