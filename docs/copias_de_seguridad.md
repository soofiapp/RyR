# Copias de seguridad y restauración (RF-6)

Módulo para proteger la base de datos SQLite local (`data/ryr_controlcenter.db`).
Se abre desde la barra lateral: **Copias de seguridad**. Solo lo pueden usar los
usuarios con permisos de edición (el mismo perfil que hace CRUD en el resto de módulos).
Los perfiles de solo lectura reciben un aviso y no entran.

## Qué permite

| Función | Cómo funciona |
|---|---|
| **Crear copia ahora** | Guarda una copia en la carpeta configurada (por defecto `backups/`). |
| **Guardar copia en…** | Guarda una copia en la ubicación que elija el usuario (otro disco, USB). Es lo que pide el RF-6. |
| **Historial** | Lista las copias de la carpeta: fecha, tipo, tamaño y archivo, de la más reciente a la más antigua. |
| **Restaurar seleccionada / desde archivo** | Vuelve a una copia anterior (de la lista o de cualquier archivo `.db`). |
| **Verificar** | Comprueba que una copia no esté dañada y sea compatible con esta versión del programa. |
| **Eliminar** | Borra una copia de la lista (con confirmación). |
| **Programación / modo automático** | Copia cada N minutos, horas o días mientras la aplicación está abierta; opcionalmente también al cerrarla. |

## Tipos de copia

Nombre de archivo: `RyR_backup_AAAAMMDD_HHMMSS_<tipo>.db`

- `manual`: creada por el usuario. Nunca se borra sola.
- `auto`: creada por el programador. Se conservan las últimas N (por defecto 10).
- `cierre`: creada al cerrar la aplicación. Cuenta junto con las `auto` para la retención.
- `prerestauracion`: el estado actual guardado justo antes de restaurar, para poder deshacer. Nunca se borra sola.

## Cómo se hace una copia (y por qué es segura)

Se usa `VACUUM INTO` de SQLite: produce una copia consistente aunque la aplicación esté en uso.
Se escribe en un archivo temporal, se verifica (`PRAGMA integrity_check`) y solo entonces recibe
su nombre definitivo, así nunca queda una copia a medias.

## Cómo se restaura

1. Se valida la copia: formato SQLite, integridad y **compatibilidad de esquema**
   (todas las tablas y columnas actuales deben existir en la copia; una copia de una versión
   anterior del programa se rechaza con un mensaje claro).
2. Se muestra un resumen (registros de la copia vs. los actuales) y se pide confirmación.
3. Se guarda una copia `prerestauracion` del estado actual.
4. Los datos se reemplazan en **una sola transacción**: o se aplica todo o no cambia nada.
5. Se verifica el resultado y se **cierra la sesión** (los datos cargados quedaron obsoletos).

**Importante:** restaurar devuelve *toda* la base (usuarios, inventario, préstamos, kárdex y
bitácora) al momento de la copia; lo registrado después se pierde. Por eso las operaciones de
copia/restauración quedan además en `data/backup.log`, que no forma parte de la base.

## Archivos locales (no van a Git)

- `backups/`: copias por defecto.
- `data/backup.properties`: configuración del modo automático.
- `data/backup.log`: registro de operaciones de copia y restauración.

## Código

- `service/BackupService`: copia, verificación, restauración, listado y retención.
- `service/BackupScheduler`: programador en segundo plano (revisa cada 30 s; si el programa estuvo
  cerrado y la copia ya estaba vencida, se hace unos 15 s después de abrirlo).
- `config/BackupConfig`: configuración persistente.
- `modelo/CopiaSeguridad`: una copia en disco.
- `controller/BackupController` + `ui/BackupVista.fxml`: pantalla.
- `MainApp`: inicia el programador al arrancar y hace la copia "al cerrar" en `stop()`.

## Limitaciones conocidas

- La copia automática solo corre mientras la aplicación está abierta (el sistema es de escritorio y sin servicios en segundo plano).
- Los eventos de bitácora usan el módulo `Usuarios y Bitacora`, porque la tabla `bitacora` solo admite los 5 módulos existentes.
