# Archivos ficticios para la revisión manual

Material de apoyo de la guía de pruebas de fases 0 a 5. Los datos no pertenecen a pacientes reales y los PDF no tienen validez clínica, legal ni tributaria. Las imágenes son diagramas ilustrativos generados mediante dibujo, no fotografías clínicas.

Los archivos se utilizan desde los formularios de carga de la aplicación. PNG/JPEG/WebP válidos están verificados por apertura; los tres PDF suman cuatro páginas y fueron renderizados y revisados visualmente. Logo PNG de 160×160. imagen-limite.png está guardada sin compresión y mide 1.76 MiB; con política temporal de 1 MiB debe rechazarse. archivo-disfrazado.jpg contiene texto: nunca debe aceptarse como imagen. formato-no-admitido.txt sirve para el rechazo de extensión.

Los PDF están en output/pdf. El PDF de María tiene dos páginas para comprobar navegación/zoom; los otros contienen los datos ficticios de Lucía y del egreso de 50.00.

La carpeta se distribuye también como datos-prueba.zip. No contiene datos privados ni credenciales. Una copia exacta se versiona en backend/docs/project/datos-prueba. El generador reproducible es backend/scripts/create-review-fixtures.py; usa Pillow, ReportLab, pypdf y Poppler, y no conecta a PostgreSQL.
