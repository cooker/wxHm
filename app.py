import os, time, shutil, urllib.parse, json, threading, io
from datetime import datetime, timedelta
from flask import Flask, render_template, request, redirect, url_for, flash, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from PIL import Image
from user_agents import parse
from werkzeug.middleware.proxy_fix import ProxyFix
from wechat_api import WeChatAPI

app = Flask(__name__)
app.secret_key = 'wxHm_secure_key_2026'
app.wsgi_app = ProxyFix(app.wsgi_app, x_proto=1, x_host=1)

# --- 数据库配置 ---
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///stats.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

UPLOAD_BASE = 'uploads'
FILES_DIR = os.path.join(UPLOAD_BASE, 'files')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD', 'admin123') 
EXPIRE_DAYS = 7             

if not os.path.exists(UPLOAD_BASE): os.makedirs(UPLOAD_BASE)
if not os.path.exists(FILES_DIR): os.makedirs(FILES_DIR)

class VisitLog(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    group_name = db.Column(db.String(50), index=True)
    date = db.Column(db.String(10), index=True)
    ip = db.Column(db.String(50))
    platform = db.Column(db.String(20))

class WeChatTemplate(db.Model):
    """微信公众号模板消息配置"""
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, comment='配置名称')
    appid = db.Column(db.String(100), nullable=False, comment='微信公众号AppID')
    secret = db.Column(db.String(200), nullable=False, comment='微信公众号Secret')
    touser = db.Column(db.String(100), nullable=False, comment='接收用户openid')
    template_id = db.Column(db.String(100), nullable=False, comment='模板ID')
    template_data = db.Column(db.Text, nullable=False, comment='模板数据JSON')
    url = db.Column(db.String(500), comment='跳转链接')
    created_at = db.Column(db.DateTime, default=datetime.now, comment='创建时间')
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now, comment='更新时间')

with app.app_context(): db.create_all()

def get_latest_wechat_template():
    """获取最新一条微信公众号模板配置"""
    return WeChatTemplate.query.order_by(WeChatTemplate.updated_at.desc(), WeChatTemplate.id.desc()).first()

def _send_wechat_template_sync(group_name: str, action: str, server_ip: str, user_label: str):
    """
    按固定格式发送微信公众号模板消息（同步实现，供后台线程调用）
    模板字段: group=群名, action=操作, server=服务器/访客IP, user=操作人员, time=触发时间
    """
    config = get_latest_wechat_template()
    if not config:
        return
    try:
        data = {
            "group": {"value": group_name or ""},
            "action": {"value": action or ""},
            "server": {"value": server_ip or ""},
            "user": {"value": user_label or ""},
            "time": {"value": datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
        }
        wechat = WeChatAPI(appid=config.appid, secret=config.secret)
        wechat.send_template_message(
            touser=config.touser,
            template_id=config.template_id,
            data=data,
            url=config.url
        )
    except Exception as e:
        print(f"自动发送微信模板消息失败: {e}")


def send_wechat_template_for_event(group_name: str, action: str, server_ip: str, user_label: str):
    """
    异步推送微信公众号模板消息，不阻塞当前请求。
    """
    def _run():
        with app.app_context():
            _send_wechat_template_sync(group_name, action, server_ip, user_label)
    t = threading.Thread(target=_run, daemon=True)
    t.start()

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
        if (now - os.path.getmtime(path)) / 86400 < EXPIRE_DAYS:
            return filename
        else:
            # 二维码已过期，删除文件并通知
            os.remove(path)
            try:
                server_ip = request.remote_addr or ''
            except Exception:
                server_ip = ''
            send_wechat_template_for_event(
                group_name=group_name,
                action='群码过期自动清理',
                server_ip=server_ip,
                user_label='系统'
            )
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

    client_ip = request.headers.get('X-Forwarded-For', request.remote_addr).split(',')[0]
    db.session.add(VisitLog(
        group_name=group_name,
        date=datetime.now().strftime('%Y-%m-%d'),
        ip=client_ip,
        platform=platform
    ))
    db.session.commit()

    # 自动发送访问通知
    send_wechat_template_for_event(
        group_name=group_name,
        action='用户访问群码',
        server_ip=client_ip,
        user_label='访客'
    )

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

    # 只统计群组目录，排除用于自定义文件的 files 目录
    groups = [
        d for d in os.listdir(UPLOAD_BASE)
        if os.path.isdir(os.path.join(UPLOAD_BASE, d)) and d != 'files'
    ]
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
    # 管理中心仅展示群组，不展示 files 目录
    groups = sorted([
        d for d in os.listdir(UPLOAD_BASE)
        if os.path.isdir(os.path.join(UPLOAD_BASE, d)) and d != 'files'
    ])
    if request.method == 'POST':
        if request.form.get('password') == ADMIN_PASSWORD:
            g_name = request.form.get('group_name', '').strip()
            file = request.files.get('file')
            if g_name and file:
                g_dir = os.path.join(UPLOAD_BASE, g_name)
                if not os.path.exists(g_dir): os.makedirs(g_dir)
                try:
                    # 读入内存再解码，避免 Mac 上流式读取导致的兼容问题
                    data = file.read()
                    img = Image.open(io.BytesIO(data)).copy()
                    img.load()
                    # 统一转为 RGB（含 RGBA/P/LA/CMYK 等）
                    if img.mode != "RGB":
                        img = img.convert("RGB")
                    base_name = f"qr_{int(time.time())}"
                    out_path_webp = os.path.join(g_dir, base_name + ".webp")
                    try:
                        img.save(out_path_webp, "WEBP", quality=80)
                    except (OSError, IOError):
                        # Mac 上若 Pillow 未带 WebP 支持则回退为 PNG
                        out_path_webp = os.path.join(g_dir, base_name + ".png")
                        img.save(out_path_webp, "PNG")
                    flash("更新成功")
                    admin_ip = request.headers.get('X-Forwarded-For', request.remote_addr).split(',')[0]
                    send_wechat_template_for_event(
                        group_name=g_name,
                        action='管理员更新群码',
                        server_ip=admin_ip,
                        user_label='管理员'
                    )
                except Exception as e:
                    flash(f"图片处理失败: {str(e)}")
                    return redirect(url_for('admin'))
        else:
            flash("密码错误")
        return redirect(url_for('admin'))
    return render_template('admin.html', groups=groups)

@app.route('/admin/upload-file', methods=['GET'])
def upload_page():
    """自定义文件上传页面"""
    files = []
    if os.path.exists(FILES_DIR):
        files = sorted([
            f for f in os.listdir(FILES_DIR)
            if os.path.isfile(os.path.join(FILES_DIR, f))
        ])
    return render_template('upload.html', files=files)

@app.route('/admin/upload-file/delete', methods=['POST'])
def delete_uploaded_file():
    """删除已上传的自定义文件"""
    if request.form.get('password') != ADMIN_PASSWORD:
        flash('密码错误')
        return redirect(url_for('upload_page'))

    filename = request.form.get('filename', '').strip()
    safe_filename = os.path.basename(filename)
    if not safe_filename:
        flash('文件名无效')
        return redirect(url_for('upload_page'))

    file_path = os.path.join(FILES_DIR, safe_filename)
    if os.path.exists(file_path) and os.path.isfile(file_path):
        try:
            os.remove(file_path)
            flash(f'删除成功: {safe_filename}')
        except Exception as e:
            flash(f'删除失败: {str(e)}')
    else:
        flash('文件不存在')

    return redirect(url_for('upload_page'))

@app.route('/admin/rename', methods=['POST'])
def rename_group():
    if request.form.get('password') == ADMIN_PASSWORD:
        old_name = request.form.get('old_name')
        new_name = request.form.get('new_name').strip()
        os.rename(os.path.join(UPLOAD_BASE, old_name), os.path.join(UPLOAD_BASE, new_name))

        # 管理员更名群码，发送通知
        admin_ip = request.headers.get('X-Forwarded-For', request.remote_addr).split(',')[0]
        send_wechat_template_for_event(
            group_name=new_name,
            action=f'管理员更名群码（{old_name} → {new_name}）',
            server_ip=admin_ip,
            user_label='管理员'
        )
    return redirect(url_for('admin'))

@app.route('/admin/delete/<group_name>', methods=['POST'])
def delete_group(group_name):
    if request.form.get('password') == ADMIN_PASSWORD:
        shutil.rmtree(os.path.join(UPLOAD_BASE, group_name))

        # 管理员删除群码，发送通知
        admin_ip = request.headers.get('X-Forwarded-For', request.remote_addr).split(',')[0]
        send_wechat_template_for_event(
            group_name=group_name,
            action='管理员删除群码',
            server_ip=admin_ip,
            user_label='管理员'
        )
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
        return redirect(url_for('upload_page'))
    
    if 'file' not in request.files:
        flash('请选择文件')
        return redirect(url_for('upload_page'))
    
    file = request.files['file']
    if file.filename == '':
        flash('请选择文件')
        return redirect(url_for('upload_page'))
    
    # 获取原始文件名（上传文件名为准）
    filename = file.filename
    
    # 确保文件名安全
    filename = os.path.basename(filename)
    if not filename:
        flash('文件名无效')
        return redirect(url_for('upload_page'))
    
    # 保存文件
    file_path = os.path.join(FILES_DIR, filename)
    try:
        file.save(file_path)
        file_url = url_for('serve_file', filename=filename, _external=True)
        flash(f'上传成功！文件已保存为: {filename}，访问链接: {file_url}')
    except Exception as e:
        flash(f'上传失败: {str(e)}')
    
    return redirect(url_for('upload_page'))

@app.route('/admin/notice', methods=['GET', 'POST'])
def notice():
    """微信公众号模板消息维护页面"""
    if request.method == 'POST':
        action = request.form.get('action', 'save')

        # 所有操作都需要先校验密码
        if request.form.get('password') != ADMIN_PASSWORD:
            flash('密码错误')
            return redirect(url_for('notice'))
        
        if action == 'save':
            # 保存配置
            config_id = request.form.get('config_id', '').strip()
            name = request.form.get('name', '').strip()
            appid = request.form.get('appid', '').strip()
            secret = request.form.get('secret', '').strip()
            touser = request.form.get('touser', '').strip()
            template_id = request.form.get('template_id', '').strip()
            template_data_str = request.form.get('template_data', '').strip()
            url = request.form.get('url', '').strip() or None
            
            # 配置名称可选，未填时使用默认名
            if not name:
                name = '配置_' + datetime.now().strftime('%Y%m%d%H%M%S')
            
            # 验证必填字段（不含 name）
            if not all([appid, secret, touser, template_id, template_data_str]):
                flash('请填写 AppID、Secret、用户ID、模板ID 和模板数据')
                return redirect(url_for('notice'))
            
            # 解析模板数据JSON
            try:
                template_data = json.loads(template_data_str)
            except json.JSONDecodeError as e:
                flash(f'模板数据JSON格式错误: {str(e)}')
                return redirect(url_for('notice'))
            
            # 验证模板数据格式
            if not isinstance(template_data, dict):
                flash('模板数据必须是JSON对象格式')
                return redirect(url_for('notice'))
            
            # 保存到数据库
            try:
                if config_id:
                    # 更新现有配置（不修改配置名称，保持原名称）
                    config = WeChatTemplate.query.get(int(config_id))
                    if config:
                        config.appid = appid
                        config.secret = secret
                        config.touser = touser
                        config.template_id = template_id
                        config.template_data = template_data_str
                        config.url = url
                        config.updated_at = datetime.now()
                        flash('配置更新成功')
                    else:
                        flash('配置不存在')
                else:
                    # 创建新配置
                    config = WeChatTemplate(
                        name=name,
                        appid=appid,
                        secret=secret,
                        touser=touser,
                        template_id=template_id,
                        template_data=template_data_str,
                        url=url
                    )
                    db.session.add(config)
                    flash('配置保存成功')
                
                db.session.commit()
            except Exception as e:
                db.session.rollback()
                flash(f'保存失败: {str(e)}')
        
        elif action == 'send':
            # 发送模板消息
            config_id = request.form.get('config_id', '').strip()
            if not config_id:
                flash('请选择要发送的配置')
                return redirect(url_for('notice'))

            config = WeChatTemplate.query.get(int(config_id))
            if not config:
                flash('配置不存在')
                return redirect(url_for('notice'))
            
            # 解析模板数据
            try:
                template_data = json.loads(config.template_data)
            except json.JSONDecodeError:
                flash('配置中的模板数据格式错误')
                return redirect(url_for('notice'))
            
            # 发送模板消息
            try:
                wechat = WeChatAPI(appid=config.appid, secret=config.secret)
                result = wechat.send_template_message(
                    touser=config.touser,
                    template_id=config.template_id,
                    data=template_data,
                    url=config.url
                )
                
                if result:
                    msgid = result.get('msgid', '')
                    flash(f'模板消息发送成功！消息ID: {msgid}')
                else:
                    flash('模板消息发送失败，请检查配置信息')
            except Exception as e:
                flash(f'发送失败: {str(e)}')

        elif action == 'load':
            # 仅根据密码加载最新一条配置到表单
            latest = WeChatTemplate.query.order_by(WeChatTemplate.updated_at.desc()).first()
            if not latest:
                flash('暂无可用配置，请先保存配置')
                return redirect(url_for('notice'))

            flash(f'已加载最新配置：{latest.name}')
            return redirect(url_for('notice', edit=latest.id))
        
        return redirect(url_for('notice'))
    
    # GET 请求：显示配置列表和表单
    configs = WeChatTemplate.query.order_by(WeChatTemplate.updated_at.desc()).all()
    edit_id = request.args.get('edit', '').strip()
    edit_config = None
    if edit_id:
        edit_config = WeChatTemplate.query.get(int(edit_id))
    
    return render_template('notice.html', configs=configs, edit_config=edit_config)

@app.route('/admin/notice/delete/<int:config_id>', methods=['POST'])
def delete_notice_config(config_id):
    """删除微信公众号模板消息配置"""
    if request.form.get('password') != ADMIN_PASSWORD:
        flash('密码错误')
        return redirect(url_for('notice'))
    
    config = WeChatTemplate.query.get(config_id)
    if config:
        db.session.delete(config)
        db.session.commit()
        flash('配置删除成功')
    else:
        flash('配置不存在')
    
    return redirect(url_for('notice'))


if __name__ == '__main__': app.run(host='0.0.0.0', port=8092)