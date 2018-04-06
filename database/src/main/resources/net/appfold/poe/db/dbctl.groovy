package net.appfold.poe.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FlywayConfiguration
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.StringUtils
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator
import org.flywaydb.core.internal.util.scanner.filesystem.FileSystemResource

import javax.sql.DataSource
import java.nio.file.Files
import java.nio.file.Paths

import static java.lang.System.*
import static java.lang.reflect.Modifier.*
import static org.flywaydb.core.internal.configuration.ConfigUtils.*
import static org.flywaydb.core.internal.database.DatabaseFactory.createDatabase
import static org.flywaydb.core.internal.util.Location.FILESYSTEM_PREFIX
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

initFlywayLogging()

def dbProps
if ((dbProps = new File(cfg('db.properties'))).isFile()) {

    LogFactory.getLog(getClass()).
        info("Loading database connection, migrations, etc. properties from ${dbProps.absolutePath}...")
    dbProps.withInputStream {
        final props = new Properties()
        props.load(it as InputStream)
        props.each { key, val -> binding[key as String] = val }
    }
}

def dbCmd
switch (dbCmd = cfg('db.cmd').toLowerCase()) {

case 'create':
case 'drop':
    "$dbCmd"(loadFlywayConfig())
    break

default:
    err.println 'Use -Ddb.cmd=[create|drop] to create or drop, respectively, the database schema and main user'
    throw new RuntimeException(dbCmd ? "Database control command '${dbCmd}' not supported!" :
                               'No database control command has been specified!')
}

//~~~

def create(FlywayConfiguration config) { asDbAdmin(config, 'create_db') }

def drop(FlywayConfiguration config) {
    def confirm = cfg('db.confirmDrop').toLowerCase()
    final yes = ['y', 'yes', 'true']

    if (!(confirm in yes)) {
        println ''
        println '!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println '!!                                                          !!'
        println '!! Serious data loss might occur when dropping the database !!'
        println '!!                                                          !!'
        println '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println ''

        def console = console()
        if (console) {
            confirm = console.readLine('Drop the database?  [y/N]: ').trim().toLowerCase()
        }
    }

    if (!(confirm in yes)) {
        err.println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    asDbAdmin(config, 'drop_db')
}

private asDbAdmin(FlywayConfiguration config, String sqlScriptPrefix) {
    def dbDialect = cfg('db.dialect')
    if (dbDialect) {
        def colonIndex = dbDialect.indexOf(':')
        if (colonIndex > -1) {
            dbDialect = dbDialect.substring(0, colonIndex)
        }
    }

    final suffixes = []
    if (config?.sqlMigrationSuffixes) {
        suffixes.addAll(config.sqlMigrationSuffixes)
    }
    if (config?.sqlMigrationSuffix && !suffixes.contains(config.sqlMigrationSuffix)) {
        suffixes.add(config.sqlMigrationSuffix)
    }
    if (suffixes.isEmpty()) {
        suffixes.add('.sql')
    }

    asDbAdmin(config) { DataSource adminDataSource ->
        final log = LogFactory.getLog(getClass())

        suffixes.each { sqlScriptSuffix ->
            final sqlScriptLocation =
                absolutePath(dbDialect, "${sqlScriptPrefix}${dbDialect ? '.' + dbDialect : ''}${sqlScriptSuffix}")
            try {
                executeSqlScript(sqlScriptLocation, config, adminDataSource)
            } catch (Exception e) {
                e instanceof FlywayException ? log.error(e.getMessage()) :
                log.error("An error occurred when executing script ${sqlScriptLocation}", e)
            }
        }
    }
}

private asDbAdmin(FlywayConfiguration config, Closure callback) {
    String adminUsername = cfg('db.adminUsername')
    char[] adminPassword = cfg('db.adminPassword').bytes

    def console = console()
    if (console) {
        if (!adminUsername) {
            adminUsername = console.readLine('DB admin username [admin]: ').trim()
            if (!adminUsername) {
                adminUsername = 'admin'
            }
        }
        if (!adminPassword) {
            adminPassword = console.readPassword('DB admin password: ')
        }
    }

    try {
        callback(new DriverDataSource(config?.classLoader ?: getClass().classLoader,

                                      // autodetected based on the URL if left empty
                                      cfg('db.jdbcDriver', { cfg('flyway.driver') }),

                                      cfg('db.jdbcServerUrl',
                                          { "jdbc:${cfg('db.dialect')}://${cfg('db.host')}:${cfg('db.port')}" }),

                                      adminUsername, new String(adminPassword)))
    } finally {
        if (adminPassword) {
            Arrays.fill(adminPassword, ' ' as char)
        }
    }
}

private executeSqlScript(String sqlScriptLocation, FlywayConfiguration config, DataSource dataSource = null) {
    final scriptResource = new FileSystemResource(sqlScriptLocation)

    createDatabase((dataSource || !config) ? new groovy.util.Proxy() {

        DataSource getDataSource() { dataSource }

    }.wrap(config) as FlywayConfiguration : config, true).withCloseable { db ->

        // Not supporting placeholders for now...
        db.createSqlScript(scriptResource, scriptResource.loadAsString(config?.encoding ?: 'UTF-8'),
                           (config?.mixed ?: true) as boolean).execute(db.mainConnection.jdbcTemplate)

    }
}

private initFlywayLogging() {
    def level
    switch (cfg('flyway.logLevel')) {
    case ['X', '-X']:
        level = DEBUG
        break
    case ['q', '-q']:
        level = WARN
        break
    default:
        level = INFO
        break
    }
    LogFactory.fallbackLogCreator = new ConsoleLogCreator(level)
}

private FlywayConfiguration loadFlywayConfig() {
    final log = LogFactory.getLog(getClass())
    final config = [:]
    final cfgEnc = cfg(CONFIG_FILE_ENCODING, 'UTF-8')
    final loadOpt = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, false)
    final loadMnd = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, true)

    // Defaults:
    config.put(LOCATIONS,
               cfg(LOCATIONS, { FILESYSTEM_PREFIX + absolutePath(cfg('db.dialect'), 'migration', Files.&isDirectory) }))

    // Configuration files:
    config.putAll(loadOpt(new File(absolutePath('conf', CONFIG_FILE_NAME))))
    config.putAll(loadOpt(new File(properties['user.home'] as String, CONFIG_FILE_NAME)))
    config.putAll(loadOpt(new File(CONFIG_FILE_NAME)))

    def configFiles = cfg(CONFIG_FILE)
    if (configFiles) {
        log.warn('configFile is deprecated and will be removed in Flyway 6.0. Use configFiles instead.')
        config.putAll(loadMnd(new File(configFiles)))
    }
    configFiles = cfg(CONFIG_FILES)
    if (configFiles) {
        for (String configFile : StringUtils.tokenizeToStringArray(configFiles, ',')) {
            config.putAll(loadMnd(new File(configFile)))
        }
    }

    // Environment variables & arguments:
    ConfigUtils.class.declaredFields.each { field ->
        final mod = field.modifiers
        if (String.class == field.type && isPublic(mod) && isStatic(mod) && isFinal(mod)) {
            def key = field.get(null) as String
            def val = cfg(key, '')
            if (val) {
                config.put(key, val)
            }
        }
    }

    config.remove(JAR_DIRS)
    config.remove(CONFIG_FILE)
    config.remove(CONFIG_FILES)
    config.remove(CONFIG_FILE_ENCODING)

    // Dump configuration (debug purposes)
    log.debug('Using configuration:')
    config.each { key, val ->
        val = PASSWORD == key ? StringUtils.trimOrPad('', (val as String).length(), '*' as char) : val
        log.debug("${key} -> ${val}")
    }

    // In Flyway versions prior to 6, org.flywaydb.core.Flyway seems to be the only class implementing the
    // org.flywaydb.core.api.configuration.FlywayConfiguration interface hence the easiest way to obtain a
    // configuration is as follows:
    final Flyway flyway = new Flyway()
    flyway.configure(config)
    return flyway
}

private String absolutePath(String parent = '', String child = '',
                            Closure<?> test = Files.&isRegularFile,
                            String basedir = cfg('basedir', { Paths.get('') as String })) {
    parent = parent ?: ''
    child = child ?: ''
    test = test ?: Files.&isRegularFile

    def path = Paths.get(basedir, parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }
    path = Paths.get(basedir, cfg('build.outputDirectory', 'target/classes'), parent, child) // Maven?
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }
    path = Paths.get(basedir, cfg('build.namespaceDirectory', { getClass().package.name.replace('.', '/') }),
                     parent, child) // under package / namespace?
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }
    path = Paths.get(basedir, cfg('build.outputDirectory', 'target/classes'),
                     cfg('build.namespaceDirectory', { getClass().package.name.replace('.', '/') }), parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    return Paths.get(basedir, parent, child).toAbsolutePath().normalize() as String
}

private String cfg(String name, def defaultOrProvider = '') {
    // Maven / Gradle project passed in bindings?
    final project = binding.variables.project

    //@fmt:off
    return ((properties[name]                                 ?:
             project?.properties?.get(name)                   ?:
             project?.hasProperty(name)?.getProperty(project) ?:
             binding.variables[name]                          ?:
             getenv(name)                                     ?://@fmt:on
             (defaultOrProvider instanceof Closure ? defaultOrProvider.call() : defaultOrProvider)) as String).trim()
}
