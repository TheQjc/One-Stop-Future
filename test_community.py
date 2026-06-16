import time
import os
import re
from playwright.sync_api import Playwright, sync_playwright, expect

def run(playwright: Playwright) -> None:
    # 1. 初始化环境与报告目录
    if not os.path.exists("test_results"):
        os.makedirs("test_results")

    print(">>> 启动浏览器，开始执行 SRS UC-02 & UC-04 回归测试...")
    # slow_mo 设为 600ms 方便答辩时演示录屏
    browser = playwright.chromium.launch(headless=False, slow_mo=600)
    context = browser.new_context()
    page = context.new_page()

    # 2. 访问首页并执行登录 (对应 SRS UC-02) [cite: 100]
    print(">>> 正在执行登录流程...")
    page.goto("http://127.0.0.1:5173/")
    page.get_by_role("link", name="登录", exact=True).click()
    page.get_by_placeholder("请输入 11 位手机号").fill("13800000000")
    page.get_by_role("button", name="获取验证码").click()
    
    # 自动识别页面上的调试验证码
    time.sleep(1.5)
    debug_text = page.get_by_text("本次测试验证码：").inner_text()
    code = re.search(r'\d+', debug_text).group()
    print(f">>> 识别到系统调试码: {code}")
    
    page.get_by_placeholder("请输入 6 位验证码").fill(code)
    page.get_by_role("button", name="登录").click()

    # 验证登录后置条件：进入社区 [cite: 91]
    expect(page.get_by_role("link", name="社区", exact=True)).to_be_visible(timeout=10000)
    page.screenshot(path="test_results/01_login_success.png")

    # 3. 发布帖子流程 (对应 SRS UC-04) [cite: 103]
    print(">>> 正在发布社区帖子...")
    page.get_by_role("link", name="社区", exact=True).click()
    page.get_by_role("link", name="发布帖子").click()
    
    # 选择标签 (枚举值 CHAT 对应“闲聊”)
    page.get_by_label("方向标签 就业考研留学闲聊").select_option("CHAT")
    
    # --- 修复超时关键点：使用模糊匹配 (exact=False) 避开冗长的占位符 ---
    title = f"Playwright回归测试_{int(time.time())}"
    page.get_by_placeholder("例如：秋招时间线复盘", exact=False).fill(title)
    page.get_by_placeholder("正文里建议写清楚背景", exact=False).fill("测试经理执行：验证全链路发帖及点赞取消功能。")
    # ---------------------------------------------------------

    page.get_by_role("button", name="立即发布").click()

    # 4. 验证发帖结果与互动 (对应 SRS UC-05) [cite: 104]
    print(">>> 正在验证帖子发布结果与点赞互动...")
    # 等待帖子出现在最新列表 [cite: 106]
    expect(page.get_by_text(title)).to_be_visible(timeout=10000)
    
    # 点击进入详情并操作点赞
    page.get_by_text(title).click()
    page.get_by_role("button", name="点赞").click()
    print(">>> 点赞操作成功")
    page.get_by_role("button", name="取消点赞").click()
    print(">>> 取消点赞成功")

    # 5. 生成报告证据
    page.screenshot(path="test_results/02_regression_final.png")
    print(f"\n✅ 测试圆满完成！")
    print(f"📊 验证项：UC-02(登录), UC-04(发帖), UC-05(点赞)")
    print(f"📸 证据已存至 test_results 文件夹。")
    
    context.close()
    browser.close()

if __name__ == "__main__":
    with sync_playwright() as playwright:
        try:
            run(playwright)
        except Exception as e:
            print(f"\n❌ 测试中断，捕获到异常：\n{e}")