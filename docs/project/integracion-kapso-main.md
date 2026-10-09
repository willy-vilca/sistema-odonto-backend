# Integración de Kapso en main

09/10/2026. El usuario confirma Kapso y Groq/openai/gpt-oss-20b para la etapa actual y solicita continuar en main. No cambia el alcance funcional 1.9 ni el plan 3.11; fases 6 y 7 conservan su cierre y fase 8 sigue pendiente.

Los dos repositorios estaban limpios y main era antecesora de kapso. Se cambió a main y se incorporó kapso mediante `git merge --ff-only kapso`, sin conflictos, conservando todos los commits y las ramas kapso de referencia.

| Repositorio | main anterior | Último commit incorporado | Commits incorporados |
|---|---|---|---|
| Backend | bb535e5 | 4a364e1 | 39 |
| Frontend | a22f1eb | 9c54c78 | 4 |

## Comprobación de integridad

- `git merge-base --is-ancestor kapso main` aprobado en ambos repositorios: todos los commits de kapso están incluidos.
- `git diff --exit-code main kapso` sin diferencias inmediatamente después de integrar.
- Árboles completos idénticos: backend `79db4e171650b8c121cb6986e1630d9a3f93e06a`; frontend `40a7c67996a174b62973896d69345e8271cc96c2`. La igualdad incluye código, recursos, migraciones, pruebas, documentación y capturas versionadas.
- Copias locales de configuración IA, Kapso y Twilio conservadas por comparación de huellas antes/después, sin publicar contenido ni huellas de los archivos privados. Siguen ignoradas por Git.
- Ambos repositorios quedaron en main. El backend añade después solamente este registro y la instantánea de la guía con la decisión vigente; no altera la implementación incorporada.

Se transfirió exactamente la implementación que pasó las 209 pruebas del cierre de fase 7. No se repitieron esas pruebas para una integración sin cambios de código. La operación de Git no ejecuta migraciones ni modifica datos de PostgreSQL. El siguiente desarrollo se hará en main.
