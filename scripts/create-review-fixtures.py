"""Create explicitly fictional files for the manual review; does not access the database."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from pypdf import PdfReader
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs' / 'datos-prueba'
PDF = OUT / 'output' / 'pdf'
PDF.mkdir(parents=True, exist_ok=True)
FONT = ROOT / 'backend' / 'src' / 'main' / 'resources' / 'fonts' / 'NotoSans-Regular.ttf'
if not FONT.exists():
    FONT = Path('C:/Windows/Fonts/arial.ttf')

def diagram(stage):
    im = Image.new('RGB', (1024, 600), '#edf2e9')
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(str(FONT), 27)
    small = ImageFont.truetype(str(FONT), 19)
    d.rounded_rectangle((30, 30, 994, 570), radius=25, fill='white')
    d.text((60, 60), 'SONRISA INTEGRAL / MATERIAL DE PRUEBA', font=f, fill='#215e4d')
    d.text((60, 115), 'María Torres Castillo - DEMO', font=small, fill='#40534b')
    d.text((60, 150), 'Registro ilustrativo ' + stage + ' / pieza 16', font=small, fill='#40534b')
    for i in range(6):
        x = 90 + i * 140
        d.rounded_rectangle((x, 235, x+95, 420), radius=28, fill='#edf2e9', outline='#215e4d', width=3)
        d.line((x+48, 250, x+48, 405), fill='#a3b4aa', width=2)
    d.ellipse((407, 290, 442, 325), fill='#e7a858' if stage == 'inicial' else '#5a9981')
    d.text((60, 475), 'Ilustración esquemática. No es una fotografía ni evidencia clínica real.', font=small, fill='#40534b')
    return im

initial = diagram('inicial')
final = diagram('posterior')
initial.save(OUT / 'registro-inicial.jpg', quality=90)
final.save(OUT / 'registro-posterior.png')
final.save(OUT / 'registro-posterior.webp', quality=90)
initial.save(OUT / 'imagen-limite.png', compress_level=0)
logo = Image.new('RGB', (160, 160), '#215e4d')
draw = ImageDraw.Draw(logo)
draw.rounded_rectangle((50, 30, 110, 125), radius=20, fill='white')
draw.text((55, 65), 'SI', font=ImageFont.truetype(str(FONT), 30), fill='#215e4d')
logo.save(OUT / 'logo-demo.png')
(OUT / 'formato-no-admitido.txt').write_text('Archivo ficticio para verificar el rechazo de formatos no admitidos.', encoding='utf8')
(OUT / 'archivo-disfrazado.jpg').write_text('Este contenido es texto, aunque el nombre termine en JPG.', encoding='utf8')

def page(c, title, lines, n):
    c.setFillColor(HexColor('#215e4d'))
    c.rect(0, 720, 595, 122, fill=1, stroke=0)
    c.setFillColor(HexColor('#ffffff'))
    c.setFont('Helvetica-Bold', 18)
    c.drawString(45, 785, 'CLÍNICA SONRISA INTEGRAL')
    c.setFont('Helvetica', 11)
    c.drawString(45, 757, 'DOCUMENTO FICTICIO - USO EXCLUSIVO DE PRUEBA')
    c.setFillColor(HexColor('#215e4d'))
    c.setFont('Helvetica-Bold', 16)
    c.drawString(45, 675, title)
    c.setFillColor(HexColor('#33463f'))
    c.setFont('Helvetica', 12)
    y = 632
    for line in lines:
        c.drawString(45, y, line)
        y -= 26
    c.setFont('Helvetica', 10)
    c.drawString(45, 60, 'Datos de demostración. Sin validez clínica, legal ni tributaria.')
    c.drawRightString(550, 60, 'Página ' + str(n))
    c.showPage()

def pdf(name, pages):
    c = canvas.Canvas(str(PDF / name), pagesize=(595, 842), pageCompression=1)
    c.setTitle('Material ficticio de revisión del sistema odontológico')
    for n, (title, lines) in enumerate(pages, 1):
        page(c, title, lines, n)
    c.save()
    doc = PdfReader(PDF / name)
    assert len(doc.pages) == len(pages)
    for p in doc.pages:
        assert 'FICTICIO' in p.extract_text()
    preview = ROOT / 'backend' / '.runtime' / 'review-fixtures'
    preview.mkdir(parents=True, exist_ok=True)
    renderer = shutil.which('pdftoppm')
    if not renderer:
        raise RuntimeError('Se requiere pdftoppm para comprobar visualmente los PDF.')
    subprocess.run([renderer, '-r', '72', '-png', str(PDF / name),
                    str(preview / name.replace('.pdf', ''))], check=True)

pdf('informe-maria-demo.pdf', [
    ('Informe documental de prueba', ['Paciente: María Torres Castillo - DEMO', 'Referencia administrativa: DNI ficticio 90000001', 'Profesional del caso: Ana Mendoza Ríos', 'Pieza vinculada en la aplicación: 16', 'Motivo: comprobar almacenamiento, visor y descarga de PDF.', 'No contiene un diagnóstico médico real.']),
    ('Continuación del informe de prueba', ['Segunda página para probar navegación y zoom.', 'Comprobar que la descarga conserva ambas páginas.', 'Relacionar este archivo con el plan de rehabilitación de María.', 'La fecha clínica se registra en el formulario de la aplicación.']),
])
pdf('consentimiento-lucia-demo.pdf', [
    ('Copia ficticia de consentimiento', ['Paciente: Lucía Paredes Gómez - DEMO', 'Responsable: Elena Paredes Gómez', 'Relación: Madre', 'Propósito: probar registro de responsable, fecha y copia adjunta.', 'No representa una autorización real ni una firma electrónica.', 'La fecha del caso se registra en la aplicación.']),
])
pdf('sustento-egreso-demo.pdf', [
    ('Sustento ficticio de egreso', ['Proveedor: Dental Demo SAC', 'Detalle: compra de guantes y material descartable de prueba.', 'Importe de referencia: S/ 50.00', 'Referencia: DEMO-EGR-001', 'Propósito: comprobar adjuntos de egresos y su recuperación.', 'No es una factura ni un comprobante tributario.']),
])
for file in sorted(OUT.rglob('*')):
    if file.is_file():
        print(str(file.relative_to(ROOT)), file.stat().st_size)
