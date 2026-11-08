-- =====================================================================
-- HU-22 - Prueba del CHECK actividades_programacion_completa_check (changeset 22)
-- =====================================================================
-- Ejecutar contra una BD con los changesets aplicados. No deja datos: todo va en una transacción que se revierte.
--   psql "$SPRING_DATASOURCE_URL" -v ON_ERROR_STOP=1 -f docs/database/pruebas/hu22-actividades-programacion-check.sql
-- Si algún caso no se comporta como se espera, el script termina con ERROR y el mensaje del caso.
-- (El proyecto aún no tiene pruebas de integración con PostgreSQL; ver pendientes técnicos.)

BEGIN;

DO $$
DECLARE
    v_evento INT;
    v_tipo   INT;
    v_sala   INT;

BEGIN
    INSERT INTO eventos (nombre, fecha_inicio, fecha_fin) VALUES ('HU22 prueba CHECK', DATE '2099-01-10', DATE '2099-01-11')
        RETURNING evento_id INTO v_evento;
    INSERT INTO tipos_actividad (evento_id, nombre) VALUES (v_evento, 'HU22 tipo') RETURNING tipo_actividad_id INTO v_tipo;
    INSERT INTO salas (evento_id, nombre, tipo, aforo_maximo) VALUES (v_evento, 'HU22 sala', 'fisica', 10)
        RETURNING sala_id INTO v_sala;

    -- ── Casos válidos ──
    INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, duracion_minutos)
    VALUES (v_evento, v_tipo, 'OK por programar', 'por_programar', 60);

    INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, fecha, hora_inicio, hora_fin, sala_id, duracion_minutos)
    VALUES (v_evento, v_tipo, 'OK programada', 'programada', DATE '2099-01-10', TIME '09:00', TIME '10:00', v_sala, 60);

    -- ── Casos inválidos: cada uno debe fallar con check_violation ──
    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado)
        VALUES (v_evento, v_tipo, 'X', 'por_programar');
        RAISE EXCEPTION 'FALLO: por_programar sin duración fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, duracion_minutos, fecha)
        VALUES (v_evento, v_tipo, 'X', 'por_programar', 60, DATE '2099-01-10');
        RAISE EXCEPTION 'FALLO: por_programar con fecha fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, duracion_minutos, hora_inicio, hora_fin)
        VALUES (v_evento, v_tipo, 'X', 'por_programar', 60, TIME '09:00', TIME '10:00');
        RAISE EXCEPTION 'FALLO: por_programar con horario fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, duracion_minutos, sala_id)
        VALUES (v_evento, v_tipo, 'X', 'por_programar', 60, v_sala);
        RAISE EXCEPTION 'FALLO: por_programar con sala fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, fecha, hora_inicio, hora_fin)
        VALUES (v_evento, v_tipo, 'X', 'programada', DATE '2099-01-10', TIME '09:00', TIME '10:00');
        RAISE EXCEPTION 'FALLO: programada sin sala fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, sala_id, duracion_minutos)
        VALUES (v_evento, v_tipo, 'X', 'programada', v_sala, 60);
        RAISE EXCEPTION 'FALLO: programada sin fecha ni horario fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO actividades (evento_id, tipo_actividad_id, nombre, estado, duracion_minutos)
        VALUES (v_evento, v_tipo, 'X', 'por_programar', 0);
        RAISE EXCEPTION 'FALLO: duración 0 fue aceptada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- Pasar una programada a por programar sin limpiar la sala también se rechaza (UPDATE).
    BEGIN
        UPDATE actividades SET estado = 'por_programar' WHERE nombre = 'OK programada' AND evento_id = v_evento;
        RAISE EXCEPTION 'FALLO: UPDATE a por_programar conservando fecha y sala fue aceptado';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- Y limpiándola sí se permite (es lo que hace "desprogramar").
    UPDATE actividades SET estado = 'por_programar', fecha = NULL, hora_inicio = NULL, hora_fin = NULL, sala_id = NULL
     WHERE nombre = 'OK programada' AND evento_id = v_evento;

    RAISE NOTICE 'HU-22: actividades_programacion_completa_check OK (2 válidos, 8 rechazos esperados, 1 desprogramación)';
END
$$;

ROLLBACK;
