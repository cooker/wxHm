import os
import time
from flask import Flask, render_template, request, redirect, url_for, flash, send_from_directory

app = Flask(__name__)
app.secret_key = 'multi_group_secure_key_2025'

# --- 配置 ---
UPLOAD_BASE = 'uploads'
ADMIN_PASSWORD = 'admin123'  # 务必修改此管理密码
EXPIRE_DAYS = 7             # 微信群码7天有效期

if not os.path.exists(UPLOAD_BASE):
    os.makedirs(UPLOAD_BASE)

def get_active_qr(group_name):
    """获取指定群组下最新且未过期的二维码文件名"""
    group_path = os.path.join(UPLOAD_BASE, group_name)
    if not os.path.exists(group_path):
        return None
    
    # 获取所有图片文件
    files = [f for f in os.listdir(group_path) if f.lower().endswith(('.png', '.jpg', '.jpeg'))]
    if not files:
        return None
    
    # 按修改时间倒序排列（最新的在前）
    files.sort(key=lambda x: os.path.getmtime(os.path.join(group_path, x)), reverse=True)
    
    now = time.time()
    for filename in files:
        path = os.path.join(group_path, filename)
        # 检查是否在有效期内
        if (now - os.path.getmtime(path)) / (24 * 3600) < EXPIRE_DAYS:
            return filename
        else:
            # 自动清理物理文件
            try:
                os.remove(path)
            except:
                pass
    return None

# --- 用户端：群组访问入口 ---
@app.route('/group/<group_name>')
def group_page(group_name):
    qr_file = get_active_qr(group_name)
    timestamp = int(time.time() * 1000)
    return render_template('index.html', group_name=group_name, qr_file=qr_file, timestamp=timestamp)

# --- 管理端：群组管理与上传 ---
@app.route('/admin', methods=['GET', 'POST'])
def admin():
    # 获取当前所有已存在的群组列表
    existing_groups = [d for d in os.listdir(UPLOAD_BASE) if os.path.isdir(os.path.join(UPLOAD_BASE, d))]
    
    if request.method == 'POST':
        pwd = request.form.get('password')
        group_input = request.form.get('group_name').strip()
        file = request.files.get('file')
        
        if pwd != ADMIN_PASSWORD:
            flash("密码校验失败，请重试！")
            return redirect(url_for('admin'))
        
        if group_input and file:
            # 为该群组创建独立文件夹
            group_dir = os.path.join(UPLOAD_BASE, group_input)
            if not os.path.exists(group_dir):
                os.makedirs(group_dir)
            
            # 保存新码，文件名带时间戳防止冲突
            ext = os.path.splitext(file.filename)[1]
            new_filename = f"qr_{int(time.time())}{ext}"
            file.save(os.path.join(group_dir, new_filename))
            
            flash(f"群组【{group_input}】二维码已更新！")
            return redirect(url_for('admin'))
            
    return render_template('admin.html', groups=existing_groups)

# --- 图片服务路由 ---
@app.route('/uploads/<group_name>/<filename>')
def serve_qr(group_name, filename):
    return send_from_directory(os.path.join(UPLOAD_BASE, group_name), filename)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8092, debug=True)