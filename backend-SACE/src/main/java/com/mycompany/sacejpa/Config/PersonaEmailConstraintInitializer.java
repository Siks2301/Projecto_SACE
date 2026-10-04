package com.mycompany.sacejpa.Config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Garantiza que {@code persona.email} sea unico en cualquier base de datos,
 * incluida una que ya exista.
 *
 * <p><b>Por que hace falta esto.</b> En {@code Persona} el correo esta declarado
 * como {@code @Column(unique = true)}, pero esa anotacion solo se materializa
 * cuando Hibernate <i>crea</i> la tabla. Con {@code spring.jpa.hibernate.ddl-auto=update}
 * sobre una tabla que ya existe, Hibernate no anade indices ni restricciones: solo
 * crea tablas y columnas ausentes. En la practica {@code unique = true} no
 * protegia nada y un correo duplicado entraba sin error, que es exactamente como
 * {@code admin@onvacation.com} quedo en dos personas (ids 2 y 4).
 *
 * <p>Se comprueba con una prueba de fuego: el INSERT duplicado no devolvia
 * error, es decir la base no estaba protegiendo el correo.
 *
 * <p><b>Por que un indice sobre {@code lower(email)} y no solo un UNIQUE.</b> Un
 * {@code UNIQUE (email)} en PostgreSQL distingue mayusculas, asi que
 * {@code ADMIN@ALELEOTOURS.COM} convive con {@code admin@aleleotours.com}. Pero el
 * login busca con {@code findByEmailIgnoreCase}, que genera {@code upper(email) =
 * upper(?)}: los dos SI colisionarian en el login. El indice va sobre
 * {@code lower(email)} para cubrir tambien las mayusculas.
 *
 * <p>Este inicializador deja el esquema igual que {@code schema_email_unico.sql},
 * pero se ejecuta solo en cada arranque. El script sigue siendo util para auditar
 * o para aplicarlo manualmente; este componente evita depender de que alguien se
 * acuerde de correrlo.
 *
 * <p>No lanza excepcion si encuentra duplicados: impedir que la aplicacion
 * arranque dejaria el sistema caido. Registra el problema con el detalle de los
 * ids afectados para que se pueda corregir, y sigue aplicando el resto.
 */
@Component
public class PersonaEmailConstraintInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PersonaEmailConstraintInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            verificarDuplicados();
            verificarCorreosVacios();

            // Coincide con la declaracion de la entidad, para una base creada
            // desde cero por ddl-auto y para una ya existente.
            if (!existeIndiceUnicoSimple()) {
                jdbcTemplate.execute(
                        "ALTER TABLE persona DROP CONSTRAINT IF EXISTS persona_email_unico");
                jdbcTemplate.execute(
                        "ALTER TABLE persona ADD CONSTRAINT persona_email_unico UNIQUE (email)");
            }
            jdbcTemplate.execute("ALTER TABLE persona ALTER COLUMN email SET NOT NULL");

            // El indice que realmente protege el login.
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS persona_email_unico_lower "
                            + "ON persona (lower(email))");

            logger.info("PersonaEmailConstraintInitializer: unicidad de persona.email "
                    + "verificada (persona_email_unico, persona_email_unico_lower).");
        } catch (Exception e) {
            logger.error("PersonaEmailConstraintInitializer: no se pudo garantizar la unicidad de "
                    + "persona.email. Si hay correos duplicados, el login por findByEmailIgnoreCase "
                    + "puede fallar para las personas afectadas. Revisa el correo en mayusculas y "
                    + "minusculas, o ejecuta schema_email_unico.sql. Motivo: {}", e.getMessage());
        }
    }

    /** Un UNIQUE (email) simple no cubre ADMIN@x.com frente a admin@x.com. */
    private boolean existeIndiceUnicoSimple() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint "
                        + "WHERE conrelid = 'persona'::regclass AND contype = 'u' "
                        + "AND pg_get_constraintdef(oid) LIKE '%(email)%' "
                        + "AND pg_get_constraintdef(oid) NOT LIKE '%lower%'",
                Integer.class);
        return total != null && total > 0;
    }

    private void verificarDuplicados() {
        // Se compara en minusculas, que es como compara el login.
        jdbcTemplate.query(
                "SELECT lower(email) AS correo, array_agg(id ORDER BY id) AS ids "
                        + "FROM persona GROUP BY lower(email) HAVING count(*) > 1",
                (rs, n) -> "  - " + rs.getString("correo") + " -> ids " + rs.getString("ids"))
                .forEach(line -> logger.error("Correo DUPLICado (sin distinguir mayusculas): " + line));

        Integer duplicados = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT lower(email) FROM persona "
                        + "GROUP BY lower(email) HAVING count(*) > 1) d",
                Integer.class);
        if (duplicados != null && duplicados > 0) {
            logger.error("Hay {} correo(s) duplicado(s) en persona. El indice unico no se creara "
                    + "hasta resolverlos; entre tanto, un login puede fallar para las personas "
                    + "afectadas.", duplicados);
        }
    }

    private void verificarCorreosVacios() {
        Integer vacios = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM persona WHERE email IS NULL OR btrim(email) = ''",
                Integer.class);
        if (vacios != null && vacios > 0) {
            logger.error("Hay {} persona(s) con correo vacio o nulo. La columna se declara NOT NULL, "
                    + "asi que revisa esos registros antes de continuar.", vacios);
        }
    }
}
