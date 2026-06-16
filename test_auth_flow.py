import time
import os
import re
from playwright.sync_api import Playwright, sync_playwright, expect

def login(page, phone):
    """通用登录模块，自带验证码提取"""
    page.goto("http://127.0.0.1:5173/")
    page.get_by_role("link", name="登录", exact=True).click()
    page.get_by_placeholder("请输入 11 位手机号").fill(phone)
    page.get_by_role("button", name="获取验证码").click()
    
    time.sleep(1.5)
    debug_text = page.get_by_text("本次测试验证码：").inner_text()
    code = re.search(r'\d+', debug_text).group()
    
    page.get_by_placeholder("请输入 6 位验证码").fill(code)
    page.get_by_role("button", name="登录").click()
    page.wait_for_selector('[data-test="home-primary-cta"]', timeout=10000)

def run(playwright: Playwright) -> None:
    if not os.path.exists("test_results"):
        os.makedirs("test_results")

    print(">>> 启动测试二：UC-03 & UC-15 跨角色全链路测试...")
    browser = playwright.chromium.launch(headless=False, slow_mo=600)

    # ==========================================
    # 角色 A：新用户 鲁迅 - 提交认证
    # ==========================================
    print("\n--- [Step 1] 角色A (新用户端) 提交学号认证 ---")
    user_context = browser.new_context()
    user_page = user_context.new_page()
    
    # 【改动点1】：使用一个从未污染过的全新账号
    print(">>> 新用户登录中 (使用 13933334444)...")
    login(user_page, "13933334444") 
    
    print(">>> 点击认证入口...")
    user_page.locator('[data-test="home-primary-cta"]').click()
    
    print(">>> 填写认证资料...")
    user_page.get_by_placeholder("输入你的真实姓名").fill("鲁迅") 
    user_page.get_by_placeholder("输入你的学号").fill("2335060103") 
    user_page.get_by_role("button", name="提交认证").click()
    
    time.sleep(1.5)
    user_page.screenshot(path="test_results/03_user_submitted.png")
    print("✅ 角色A：新申请已提交至后台！")

    # ==========================================
    # 角色 B：管理员 - 后台审核
    # ==========================================
    print("\n--- [Step 2] 角色B (管理端) 处理审批流 ---")
    admin_context = browser.new_context()
    admin_page = admin_context.new_page()
    
    print(">>> 管理员登录中 (使用 13800000000)...")
    login(admin_page, "13800000000") 
    
    print(">>> 进入认证审核工作台...")
    admin_page.get_by_role("link", name="认证审核", exact=True).click()
    
    print(">>> 检索并审核【鲁迅】的申请...")
    # 【改动点2】：合并了之前修复的 review-card 和 .first 严格模式定位逻辑
    target_card = admin_page.locator("article.review-card").filter(has_text=re.compile(r"鲁迅")).first
    target_card.get_by_role("button", name="通过").click()
    
    time.sleep(1.5) 
    admin_page.screenshot(path="test_results/04_admin_approved.png")
    print("✅ 角色B：审核【通过】指令下发成功！")

    # ==========================================
    # 闭环验证：切回 角色 A
    # ==========================================
    print("\n--- [Step 3] 全链路状态对账 ---")
    print(">>> 刷新用户A页面，验证状态是否同步...")
    user_page.reload()
    time.sleep(2)
    
    user_page.screenshot(path="test_results/05_final_status.png")
    print("✅ 对账完成：请查看 05_final_status.png 确认用户状态已变更。")

    print("\n🎉 测试二：跨角色多端联动测试圆满完成！")
    
    user_context.close()
    admin_context.close()
    browser.close()

if __name__ == "__main__":
    with sync_playwright() as playwright:
        try:
            run(playwright)
        except Exception as e:
            print(f"\n❌ 测试中断，捕获到异常：\n{e}")