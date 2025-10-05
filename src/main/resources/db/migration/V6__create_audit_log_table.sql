-- Tabla de auditoría para registro de acciones de seguridad
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    identifier VARCHAR(100),
    reason VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para mejorar consultas de auditoría
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_result ON audit_log(result);
CREATE INDEX idx_audit_log_ip_address ON audit_log(ip_address);

-- Comentarios para documentación
COMMENT ON TABLE audit_log IS 'Registro de auditoría de acciones de seguridad y autenticación';
COMMENT ON COLUMN audit_log.action IS 'Tipo de acción: LOGIN_ATTEMPT, LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, REFRESH_TOKEN, etc.';
COMMENT ON COLUMN audit_log.result IS 'Resultado de la acción: SUCCESS, FAILURE, ERROR';
COMMENT ON COLUMN audit_log.ip_address IS 'Dirección IP del cliente';
COMMENT ON COLUMN audit_log.user_agent IS 'User-Agent del navegador/cliente';
COMMENT ON COLUMN audit_log.identifier IS 'Email o username usado en el intento';
COMMENT ON COLUMN audit_log.reason IS 'Razón del resultado (ej: Invalid credentials, Account locked, etc.)';
