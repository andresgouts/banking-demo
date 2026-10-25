package com.banking.demo.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilidad centralizada de seguridad para validar la pertenencia de cuentas y movimientos
 * al usuario autenticado única y exclusivamente por medio de su Id.
 */
@Slf4j
public final class SecurityUtils {

    public static final String ACCESS_DENIED_MESSAGE = "No tiene permisos para operar sobre una cuenta que no le pertenece";

    private SecurityUtils() {
        // Clase utilitaria privada
    }

    /**
     * Obtiene el ID del usuario autenticado a partir del SecurityContextHolder.
     *
     * @return el ID si el usuario está autenticado y no es anónimo; null en caso contrario.
     */
    public static String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !auth.getName().isBlank()
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName().trim();
        }
        return null;
    }

    public static Long getAuthenticatedId() {
        String userId = getAuthenticatedUserId();
        if (userId != null && userId.matches("\\d+")) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Valida que el recurso a registrar o consultar pertenezca única y exclusivamente
     * al Id del usuario autenticado.
     *
     * @param ownerId Id propietario del recurso (cuenta o movimiento).
     * @throws AccessDeniedException si el usuario autenticado no coincide con el propietario del recurso.
     */
    public static void validateAuthenticatedOwner(Long ownerId) {
        if (ownerId == null) {
            return;
        }
        validateAuthenticatedOwner(String.valueOf(ownerId));
    }

    public static void validateAuthenticatedOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return;
        }
        String authenticatedUser = getAuthenticatedUserId();
        if (authenticatedUser != null && !authenticatedUser.equals(ownerId.trim())) {
            log.warn("Acceso denegado: el usuario autenticado [{}] intento operar sobre recurso del cliente [{}]",
                    authenticatedUser, ownerId);
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }
}
