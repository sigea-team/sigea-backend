package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.model.Auditoria;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación con Criteria API de la búsqueda filtrada del log de auditoría.
 * <p>
 * Se usa Criteria en lugar de JPQL con {@code (:param IS NULL OR ...)} porque PostgreSQL no puede
 * inferir el tipo de un parámetro nulo, lo que provoca errores en filtros opcionales de fecha.
 * </p>
 */
public class AuditoriaRepositoryCustomImpl implements AuditoriaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Auditoria> buscar(FiltroAuditoria filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Conteo total (sin fetch)
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Auditoria> countRoot = countQuery.from(Auditoria.class);
        Join<Auditoria, Usuario> countUsuario = countRoot.join("usuario", JoinType.LEFT);
        Join<Usuario, Persona> countPersona = countUsuario.join("persona", JoinType.LEFT);
        countQuery.select(cb.count(countRoot))
                .where(predicados(cb, countRoot, countUsuario, countPersona, filtro));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2. Página de datos, trayendo usuario y persona en la misma consulta (evita N+1)
        CriteriaQuery<Auditoria> dataQuery = cb.createQuery(Auditoria.class);
        Root<Auditoria> root = dataQuery.from(Auditoria.class);
        Join<Auditoria, Usuario> usuario = (Join<Auditoria, Usuario>) root.<Auditoria, Usuario>fetch("usuario", JoinType.LEFT);
        Join<Usuario, Persona> persona = (Join<Usuario, Persona>) usuario.<Usuario, Persona>fetch("persona", JoinType.LEFT);
        dataQuery.select(root)
                .where(predicados(cb, root, usuario, persona, filtro))
                .orderBy(cb.desc(root.get("fechaHora")), cb.desc(root.get("id")));

        List<Auditoria> contenido = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(contenido, pageable, total);
    }

    private Predicate[] predicados(CriteriaBuilder cb,
                                   Root<Auditoria> root,
                                   Join<Auditoria, Usuario> usuario,
                                   Join<Usuario, Persona> persona,
                                   FiltroAuditoria f) {
        List<Predicate> p = new ArrayList<>();
        if (f == null) {
            return new Predicate[0];
        }
        if (f.usuarioId() != null) {
            p.add(cb.equal(usuario.get("id"), f.usuarioId()));
        }
        if (StringUtils.hasText(f.correo())) {
            p.add(cb.like(cb.lower(persona.<String>get("correo")), "%" + f.correo().trim().toLowerCase() + "%"));
        }
        if (StringUtils.hasText(f.accion())) {
            p.add(cb.equal(root.get("accion"), f.accion().trim().toUpperCase()));
        }
        if (StringUtils.hasText(f.entidad())) {
            p.add(cb.equal(cb.lower(root.<String>get("entidad")), f.entidad().trim().toLowerCase()));
        }
        if (f.desde() != null) {
            p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaHora"), f.desde().atStartOfDay()));
        }
        if (f.hasta() != null) {
            // "hasta" inclusivo: todo el día, es decir < (hasta + 1 día) a las 00:00
            p.add(cb.lessThan(root.<LocalDateTime>get("fechaHora"), f.hasta().plusDays(1).atStartOfDay()));
        }
        return p.toArray(new Predicate[0]);
    }
}
