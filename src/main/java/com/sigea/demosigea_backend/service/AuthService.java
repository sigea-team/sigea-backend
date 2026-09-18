package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.auth.LoginRequest;
import com.sigea.demosigea_backend.dto.auth.LoginResponse;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionRequest;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionResponse;
import com.sigea.demosigea_backend.dto.auth.RegistroRequest;
import com.sigea.demosigea_backend.dto.auth.RegistroResponse;
import com.sigea.demosigea_backend.dto.auth.VerificarCorreoResponse;
import com.sigea.demosigea_backend.exception.CorreoNoVerificadoException;
import com.sigea.demosigea_backend.exception.CredencialesInvalidasException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.TokenInvalidoException;
import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoToken;
import com.sigea.demosigea_backend.model.TokenRecuperacion;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.TokenRecuperacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de autenticación y registro de usuarios del sistema SIGEA.
 * <p>
 * Contiene la lógica de negocio para el registro de personas y cuentas de usuario,
 * el inicio de sesión con validación de credenciales y estado de verificación,
 * la verificación de correo electrónico mediante tokens UUID y el reenvío
 * de enlaces de verificación.
 * </p>
 * <p>
 * Todas las operaciones de escritura están gestionadas con {@code @Transactional}
 * para garantizar la consistencia de datos entre las tablas {@code personas},
 * {@code usuarios} y {@code tokens_recuperacion}.
 * </p>
 *
 * @author SIGEA Team
 * @version 1.0
 * @see AuthController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Repositorio de acceso a datos de personas. */
    private final PersonaRepository personaRepository;
    /** Repositorio de acceso a datos de usuarios. */
    private final UsuarioRepository usuarioRepository;
    /** Repositorio de acceso a datos de roles. */
    private final RolRepository rolRepository;
    /** Repositorio de acceso a datos de tokens de recuperación/verificación. */
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    /** Codificador de contraseñas (BCrypt). */
    private final PasswordEncoder passwordEncoder;
    /** Proveedor de tokens JWT para la generación y validación. */
    private final JwtTokenProvider jwtTokenProvider;
    /** Servicio de envío de correos electrónicos. */
    private final EmailService emailService;

    /** Nombre del rol inicial asignado a los nuevos usuarios (configurable vía {@code app.roles.default-initial}). */
    @Value("${app.roles.default-initial:PARTICIPANTE}")
    private String defaultRoleName;

    /**
     * Registra un nuevo usuario en el sistema SIGEA.
     * <p>
     * Valida la unicidad del correo electrónico y número de documento, crea los registros
     * de {@link Persona} y {@link Usuario}, asigna el rol inicial por defecto, genera un
     * token de verificación de correo y envía el enlace de verificación por email.
     * </p>
     *
     * @param request datos del formulario de registro con validaciones Bean Validation
     * @return {@link RegistroResponse} con el ID de usuario, nombre de usuario, correo y mensaje informativo
     * @throws RecursoDuplicadoException si el correo, documento o nombre de usuario ya existen en el sistema
     */
    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        String correoNormalizado = request.correo().trim().toLowerCase();
        String numeroDocNormalizado = request.numeroDocumento().trim();

        // Criterio 2: Validación de existencia previa de correo o documento
        if (personaRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta registrada con el correo electrónico ingresado.");
        }

        if (personaRepository.existsByNumeroDocumento(numeroDocNormalizado)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta registrada con el número de documento ingresado.");
        }

        // Determinar y validar nombre de usuario
        String nombreUsuario = resolverNombreUsuario(request.nombreUsuario(), correoNormalizado);
        if (usuarioRepository.existsByNombreUsuarioIgnoreCase(nombreUsuario)) {
            throw new RecursoDuplicadoException("El nombre de usuario '" + nombreUsuario + "' ya se encuentra en uso.");
        }

        // Obtener o crear el rol inicial
        Rol rolInicial = rolRepository.findByNombreIgnoreCase(defaultRoleName)
                .orElseGet(() -> {
                    log.info("Creando rol inicial por defecto: {}", defaultRoleName);
                    Rol nuevoRol = Rol.builder()
                            .nombre(defaultRoleName)
                            .descripcion("Rol inicial para participantes y asistentes registrados")
                            .build();
                    return rolRepository.save(nuevoRol);
                });

        // 1. Guardar Persona
        Persona persona = Persona.builder()
                .tipoDocumento(request.tipoDocumento().trim().toUpperCase())
                .numeroDocumento(numeroDocNormalizado)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(correoNormalizado)
                .telefono(StringUtils.hasText(request.telefono()) ? request.telefono().trim() : null)
                .afiliacionInstitucional(StringUtils.hasText(request.afiliacionInstitucional()) ? request.afiliacionInstitucional().trim() : null)
                .build();
        Persona personaGuardada = personaRepository.save(persona);

        // 2. Guardar Usuario
        Set<Rol> roles = new HashSet<>();
        roles.add(rolInicial);

        Usuario usuario = Usuario.builder()
                .persona(personaGuardada)
                .nombreUsuario(nombreUsuario)
                .contrasenaHash(passwordEncoder.encode(request.contrasena()))
                .correoVerificado(false)
                .estado(EstadoUsuario.activo)
                .fechaRegistro(LocalDateTime.now())
                .roles(roles)
                .build();
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 3. Generar Token de Verificación
        String tokenUuid = UUID.randomUUID().toString();
        TokenRecuperacion tokenVerificacion = TokenRecuperacion.builder()
                .usuario(usuarioGuardado)
                .token(tokenUuid)
                .tipo(TipoToken.verificacion)
                .fechaGeneracion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(24))
                .usado(false)
                .build();
        tokenRecuperacionRepository.save(tokenVerificacion);

        // 4. Enviar correo de verificación
        emailService.enviarCorreoVerificacion(
                personaGuardada.getCorreo(),
                personaGuardada.getNombres(),
                tokenUuid
        );

        return new RegistroResponse(
                usuarioGuardado.getId(),
                usuarioGuardado.getNombreUsuario(),
                personaGuardada.getCorreo(),
                "Cuenta creada exitosamente. Se ha enviado un enlace de verificación a su correo electrónico. Por favor confírmelo para habilitar el acceso a la plataforma.",
                true
        );
    }

    /**
     * Verifica el correo electrónico de un usuario mediante un token UUID.
     * <p>
     * Busca el token de tipo {@code verificacion} en la base de datos, valida que no haya sido
     * usado previamente ni esté expirado, lo marca como utilizado y actualiza el campo
     * {@code correo_verificado} del usuario a {@code true}.
     * </p>
     *
     * @param tokenString cadena UUID del token de verificación recibido por correo
     * @return {@link VerificarCorreoResponse} con mensaje de éxito e indicador de verificación
     * @throws TokenInvalidoException si el token es vacío, no existe, ya fue usado o ha expirado
     */
    @Transactional
    public VerificarCorreoResponse verificarCorreo(String tokenString) {
        if (!StringUtils.hasText(tokenString)) {
            throw new TokenInvalidoException("El token de verificación no puede estar vacío.");
        }

        TokenRecuperacion token = tokenRecuperacionRepository.findByTokenAndTipo(tokenString.trim(), TipoToken.verificacion)
                .orElseThrow(() -> new TokenInvalidoException("El enlace o token de verificación proporcionado no es válido."));

        if (Boolean.TRUE.equals(token.getUsado())) {
            throw new TokenInvalidoException("Este enlace de verificación ya fue utilizado previamente.");
        }

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new TokenInvalidoException("El enlace de verificación ha expirado. Por favor solicite el reenvío de un nuevo enlace.");
        }

        token.setUsado(true);
        tokenRecuperacionRepository.save(token);

        Usuario usuario = token.getUsuario();
        usuario.setCorreoVerificado(true);
        usuarioRepository.save(usuario);

        return new VerificarCorreoResponse(
                "¡Correo verificado con éxito! Su cuenta ha sido habilitada. Ya puede iniciar sesión en la plataforma SIGEA.",
                true
        );
    }

    /**
     * Autentica un usuario en el sistema mediante su identificador y contraseña.
     * <p>
     * Busca al usuario por correo electrónico o nombre de usuario, valida la contraseña
     * contra el hash almacenado, verifica que el correo esté confirmado y que la cuenta
     * esté activa. Si todo es correcto, genera y retorna un token JWT Bearer.
     * </p>
     *
     * @param request credenciales de inicio de sesión (identificador + contraseña)
     * @return {@link LoginResponse} con token JWT, datos del usuario y lista de roles
     * @throws CredencialesInvalidasException si el usuario no existe, la contraseña no coincide o la cuenta no está activa
     * @throws CorreoNoVerificadoException si el correo electrónico del usuario aún no ha sido verificado
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identificador = request.identificador().trim();

        Usuario usuario = usuarioRepository.findByIdentificador(identificador)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo/usuario y contraseña."));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            throw new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo/usuario y contraseña.");
        }

        // Criterio 4: Verificar si el correo ya fue confirmado
        if (!Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            throw new CorreoNoVerificadoException(
                    "No se puede iniciar sesión: Su correo electrónico aún no ha sido verificado. Por favor revise su bandeja de entrada o solicite el reenvío del enlace de verificación.",
                    usuario.getPersona().getCorreo()
            );
        }

        if (usuario.getEstado() != EstadoUsuario.activo) {
            throw new CredencialesInvalidasException("La cuenta de usuario no se encuentra activa (estado: " + usuario.getEstado() + ").");
        }

        List<String> rolesNombres = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .toList();

        String tokenJwt = jwtTokenProvider.generateToken(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getPersona().getCorreo(),
                rolesNombres
        );

        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        return new LoginResponse(
                tokenJwt,
                "Bearer",
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getPersona().getCorreo(),
                nombreCompleto,
                rolesNombres
        );
    }

    /**
     * Reenvía el enlace de verificación de correo electrónico a un usuario.
     * <p>
     * Busca al usuario por identificador, invalida todos los tokens de verificación previos
     * que no hayan sido usados, genera un nuevo token con vigencia de 24 horas y envía
     * el correo de verificación. Si la cuenta ya está verificada, retorna un mensaje informativo.
     * </p>
     *
     * @param request datos con el identificador (correo o nombre de usuario) de la cuenta
     * @return {@link ReenviarVerificacionResponse} con mensaje de confirmación y correo destinatario
     * @throws RecursoNoEncontradoException si no se encuentra un usuario con el identificador proporcionado
     */
    @Transactional
    public ReenviarVerificacionResponse reenviarVerificacion(ReenviarVerificacionRequest request) {
        String identificador = request.identificador().trim();

        Usuario usuario = usuarioRepository.findByIdentificador(identificador)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró ningún usuario con el correo o nombre de usuario proporcionado."));

        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            return new ReenviarVerificacionResponse(
                    "La cuenta ya tiene el correo electrónico verificado. Puede iniciar sesión directamente.",
                    usuario.getPersona().getCorreo()
            );
        }

        // Invalidar tokens previos activos de verificación
        List<TokenRecuperacion> tokensPrevios = tokenRecuperacionRepository.findByUsuarioAndTipoAndUsadoFalse(usuario, TipoToken.verificacion);
        for (TokenRecuperacion t : tokensPrevios) {
            t.setUsado(true);
        }
        tokenRecuperacionRepository.saveAll(tokensPrevios);

        // Generar nuevo token
        String nuevoTokenUuid = UUID.randomUUID().toString();
        TokenRecuperacion nuevoToken = TokenRecuperacion.builder()
                .usuario(usuario)
                .token(nuevoTokenUuid)
                .tipo(TipoToken.verificacion)
                .fechaGeneracion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(24))
                .usado(false)
                .build();
        tokenRecuperacionRepository.save(nuevoToken);

        // Enviar nuevo correo
        emailService.enviarCorreoVerificacion(
                usuario.getPersona().getCorreo(),
                usuario.getPersona().getNombres(),
                nuevoTokenUuid
        );

        return new ReenviarVerificacionResponse(
                "Se ha enviado un nuevo enlace de verificación a su correo electrónico.",
                usuario.getPersona().getCorreo()
        );
    }

    /**
     * Resuelve el nombre de usuario a utilizar para la nueva cuenta.
     * <p>
     * Si el usuario proporcionó un nombre de usuario, lo normaliza en minúsculas.
     * De lo contrario, genera uno automáticamente a partir de la parte local del correo electrónico,
     * eliminando caracteres especiales y truncando a un máximo de 50 caracteres.
     * </p>
     *
     * @param nombreUsuarioPropuesto nombre de usuario propuesto por el usuario (puede ser {@code null} o vacío)
     * @param correo correo electrónico del usuario, usado como base para generar el nombre de usuario
     * @return nombre de usuario normalizado en minúsculas
     */
    private String resolverNombreUsuario(String nombreUsuarioPropuesto, String correo) {
        if (StringUtils.hasText(nombreUsuarioPropuesto)) {
            return nombreUsuarioPropuesto.trim().toLowerCase();
        }
        String base = correo.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", "");
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        return base.toLowerCase();
    }
}
