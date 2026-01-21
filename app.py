import os, time, shutil, urllib.parse
from datetime import datetime, timedelta
from flask import Flask, render_template, request, redirect, url_for, flash, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from PIL import Image
from user_agents import parse
from werkzeug.middleware.proxy_fix import ProxyFix

app = Flask(__name__)
app.secret_key = 'wxHm_secure_key_2026'
app.wsgi_app = ProxyFix(app.wsgi_app, x_proto=1, x_host=1)

# --- 数据库配置 ---
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///stats.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

UPLOAD_BASE = 'uploads'
FILES_DIR = os.path.join(UPLOAD_BASE, 'files')
ADMIN_PASSWORD = 'admin123' 
EXPIRE_DAYS = 7             

if not os.path.exists(UPLOAD_BASE): os.makedirs(UPLOAD_BASE)
if not os.path.exists(FILES_DIR): os.makedirs(FILES_DIR)

class VisitLog(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    group_name = db.Column(db.String(50), index=True)
    date = db.Column(db.String(10), index=True)
    ip = db.Column(db.String(50))
    platform = db.Column(db.String(20))

with app.app_context(): db.create_all()

@app.after_request
def add_header(response):
    response.headers['Referrer-Policy'] = 'no-referrer-when-downgrade'
    return response

def get_active_qr(group_name):
    group_path = os.path.join(UPLOAD_BASE, group_name)
    if not os.path.exists(group_path): return None
    files = [f for f in os.listdir(group_path) if f.lower().endswith(('.webp', '.png', '.jpg'))]
    if not files: return None
    files.sort(key=lambda x: os.path.getmtime(os.path.join(group_path, x)), reverse=True)
    now = time.time()
    for filename in files:
        path = os.path.join(group_path, filename)
        if (now - os.path.getmtime(path)) / 86400 < EXPIRE_DAYS: return filename
        else: os.remove(path)
    return None

# --- 路由：展示页 ---
@app.route('/group/<group_name>')
def group_page(group_name):
    qr_file = get_active_qr(group_name)
    
    # 记录统计：解析设备
    ua = parse(request.headers.get('User-Agent', ''))
    if ua.is_mobile:
        platform = 'iOS' if 'iPhone' in ua.ua_string or 'iPad' in ua.ua_string else 'Android'
    else:
        if 'Windows' in ua.ua_string: platform = 'Windows'
        elif 'Macintosh' in ua.ua_string: platform = 'Mac'
        elif 'Linux' in ua.ua_string: platform = 'Linux'
        else: platform = 'Other'

    db.session.add(VisitLog(group_name=group_name, date=datetime.now().strftime('%Y-%m-%d'), 
                            ip=request.headers.get('X-Forwarded-For', request.remote_addr).split(',')[0],
                            platform=platform))
    db.session.commit()

    wsrv_url = ""
    if qr_file:
        raw_url = f"https://{request.host}/uploads/{group_name}/{qr_file}"
        wsrv_url = f"https://wsrv.nl/?url={urllib.parse.quote(raw_url, safe='')}&we=1&v={int(time.time())}"
    
    return render_template('index.html', group_name=group_name, qr_file=qr_file, wsrv_url=wsrv_url)

# --- 路由：统计看板 ---
@app.route('/admin/stats')
def stats():
    today = datetime.now().strftime('%Y-%m-%d')
    dates = [(datetime.now() - timedelta(days=i)).strftime('%Y-%m-%d') for i in range(7)]
    dates.reverse()

    groups = [d for d in os.listdir(UPLOAD_BASE) if os.path.isdir(os.path.join(UPLOAD_BASE, d))]
    stats_result = {}
    for g in groups:
        trend = []
        for d in dates:
            pv = VisitLog.query.filter_by(group_name=g, date=d).count()
            uv = db.session.query(VisitLog.ip).filter_by(group_name=g, date=d).distinct().count()
            trend.append({'date': d, 'pv': pv, 'uv': uv})
        
        pie = [{"name": p[0], "value": p[1]} for p in db.session.query(VisitLog.platform, db.func.count(VisitLog.id))
               .filter_by(group_name=g, date=today).group_by(VisitLog.platform).all()]
        stats_result[g] = {"trend": trend, "pie": pie}

    # 清理过期数据
    VisitLog.query.filter(VisitLog.date < (datetime.now()-timedelta(days=7)).strftime('%Y-%m-%d')).delete()
    db.session.commit()
    return render_template('stats.html', stats_data=stats_result)

@app.route('/admin', methods=['GET', 'POST'])
def admin():
    groups = sorted([d for d in os.listdir(UPLOAD_BASE) if os.path.isdir(os.path.join(UPLOAD_BASE, d))])
    if request.method == 'POST':
        if request.form.get('password') == ADMIN_PASSWORD:
            g_name = request.form.get('group_name', '').strip()
            file = request.files.get('file')
            if g_name and file:
                g_dir = os.path.join(UPLOAD_BASE, g_name)
                if not os.path.exists(g_dir): os.makedirs(g_dir)
                img = Image.open(file)
                if img.mode in ("RGBA", "P"): img = img.convert("RGB")
                img.save(os.path.join(g_dir, f"qr_{int(time.time())}.webp"), "WEBP", quality=80)
                flash("更新成功")
        else: flash("密码错误")
        return redirect(url_for('admin'))
    return render_template('admin.html', groups=groups)

@app.route('/admin/rename', methods=['POST'])
def rename_group():
    if request.form.get('password') == ADMIN_PASSWORD:
        os.rename(os.path.join(UPLOAD_BASE, request.form.get('old_name')), os.path.join(UPLOAD_BASE, request.form.get('new_name').strip()))
    return redirect(url_for('admin'))

@app.route('/admin/delete/<group_name>', methods=['POST'])
def delete_group(group_name):
    if request.form.get('password') == ADMIN_PASSWORD: shutil.rmtree(os.path.join(UPLOAD_BASE, group_name))
    return redirect(url_for('admin'))

@app.route('/uploads/<group_name>/<filename>')
def serve_qr(group_name, filename): return send_from_directory(os.path.join(UPLOAD_BASE, group_name), filename)

@app.route('/')
def home():
    """项目介绍首页"""
    github_url = "https://github.com/cooker/wxHm" # 替换为你的真实地址
    return render_template('home.html', github_url=github_url)

@app.route('/<filename>')
def serve_file(filename):
    """提供上传的文件访问（根路径访问）"""
    # 排除包含斜杠的路径（多段路径应该由其他路由处理）
    if '/' in filename:
        from flask import abort
        abort(404)
    
    # 安全检查：防止路径遍历
    safe_filename = os.path.basename(filename)
    file_path = os.path.join(FILES_DIR, safe_filename)
    
    # 检查文件是否存在
    if os.path.exists(file_path) and os.path.isfile(file_path):
        return send_from_directory(FILES_DIR, safe_filename)
    else:
        # 文件不存在，返回404（让Flask继续匹配其他路由，但实际上Flask不会继续匹配）
        from flask import abort
        abort(404)

@app.route('/upload', methods=['POST'])
def upload_file():
    """上传自定义文件（需要管理员密码）"""
    # 验证密码
    if request.form.get('password') != ADMIN_PASSWORD:
        flash('密码错误')
        return redirect(url_for('admin'))
    
    if 'file' not in request.files:
        flash('请选择文件')
        return redirect(url_for('admin'))
    
    file = request.files['file']
    if file.filename == '':
        flash('请选择文件')
        return redirect(url_for('admin'))
    
    # 获取原始文件名
    filename = file.filename
    # 如果用户提供了自定义文件名，使用自定义文件名，否则使用原始文件名
    custom_name = request.form.get('custom_name', '').strip()
    if custom_name:
        filename = custom_name
    
    # 确保文件名安全
    filename = os.path.basename(filename)
    if not filename:
        flash('文件名无效')
        return redirect(url_for('admin'))
    
    # 保存文件
    file_path = os.path.join(FILES_DIR, filename)
    try:
        file.save(file_path)
        file_url = url_for('serve_file', filename=filename, _external=True)
        flash(f'上传成功！文件已保存为: {filename}，访问链接: {file_url}')
    except Exception as e:
        flash(f'上传失败: {str(e)}')
    
    return redirect(url_for('admin'))


if __name__ == '__main__': app.run(host='0.0.0.0', port=8092)