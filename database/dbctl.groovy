import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FlywayConfiguration
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.StringUtils
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator
import org.flywaydb.core.internal.util.scanner.filesystem.FileSystemResource

import javax.sql.DataSource
import java.nio.file.Files
import java.nio.file.Paths

import static OptValue.*
import static java.lang.System.*
import static java.lang.reflect.Modifier.*
import static org.flywaydb.core.api.logging.LogFactory.fallbackLogCreator
import static org.flywaydb.core.api.logging.LogFactory.getLog
import static org.flywaydb.core.internal.configuration.ConfigUtils.*
import static org.flywaydb.core.internal.database.DatabaseFactory.createDatabase
import static org.flywaydb.core.internal.util.Location.FILESYSTEM_PREFIX
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

final log = initFlywayLogging()

final dbProps
if ((dbProps = new File(env('ds.properties'))).isFile()) {

    log.debug("Loading database connection, migrations, etc. properties from ${dbProps.absolutePath}...")
    dbProps.withInputStream {
        final props = new Properties()
        props.load(it as InputStream)
        props.each { key, val -> binding[key as String] = val }
    }
}

final operations = [:] as LinkedHashMap

def dbOpt = env('db.drop')
if (dbOpt) {
    operations['drop'] = dbOpt
}

dbOpt = env('db.create')
if (dbOpt) {
    operations['create'] = dbOpt
}

if (operations.isEmpty()) {
    log.info("Use -Ddb.drop=[${opts(YES, FORCE)}] to drop " +
             "and -Ddb.create=[${opts(YES)}] to create the database schema and main user")
} else {
    asDbAdmin(loadFlywayConfig()) { adminDataSource, config ->

        operations.each { operation, option ->

            try {
                "$operation"(adminDataSource, config, option)
            } catch (ex) {
                handle(ex, { "An error occurred while executing operation ${operation}" })
            }
        }
    }
}

//~~~

def create(DataSource dataSource, FlywayConfiguration config, def createOpt) {
    executeSqlScripts(dataSource, config, 'create_db')
}

def drop(DataSource dataSource, FlywayConfiguration config, def dropOpt) {
    final log = getLog(getClass())

    if (YES.eq(dropOpt)) {
        log.info("""

!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!!                                                          !!
!! Serious data loss might occur when dropping the database !!
!!                                                          !!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
""")

        def console = console()
        if (console && YES.eq(console.readLine('Drop the database? (y/N): ').trim())) {
            dropOpt = FORCE
        }
    }

    if (FORCE.eq(dropOpt)) {
        executeSqlScripts(dataSource, config, 'drop_db')
    } else {
        log.error("Use -Ddb.drop=[${opts(YES, FORCE)}] to drop the database schema and main user")
    }
}

private asDbAdmin(FlywayConfiguration config, Closure callback) {
    String adminUsername = env('db.adminUsername')
    char[] adminPassword = env('db.adminPassword').bytes

    def console = console()
    if (console) {
        if (!adminUsername) {
            adminUsername = console.readLine('DB admin username (root): ').trim()
            if (!adminUsername) {
                adminUsername = 'root'
            }
        }
        if (!adminPassword) {
            adminPassword = console.readPassword('DB admin password: ')
        }
    }

    try {
        callback(new DriverDataSource(config?.classLoader ?: getClass().classLoader,

                                      // autodetected based on the URL if left empty
                                      env('db.driver', { env('flyway.driver') }),

                                      env('db.serverUrl',
                                          { "jdbc:${env('db.dialect')}://${env('db.host')}:${env('db.port')}" }),

                                      adminUsername, new String(adminPassword)), config)
    } finally {
        if (adminPassword) {
            Arrays.fill(adminPassword, ' ' as char)
        }
    }
}

private executeSqlScripts(DataSource dataSource, FlywayConfiguration config, String... sqlScriptPrefixes) {
    if (!sqlScriptPrefixes) {
        return
    }

    final log = getLog(getClass())
    final dry = YES.eq(env('db.dryRun'))

    final sqlScriptSuffixes = []
    if (config?.sqlMigrationSuffixes) {
        sqlScriptSuffixes.addAll(config.sqlMigrationSuffixes)
    }
    if (config?.sqlMigrationSuffix && !sqlScriptSuffixes.contains(config.sqlMigrationSuffix)) {
        sqlScriptSuffixes.add(config.sqlMigrationSuffix)
    }
    if (sqlScriptSuffixes.isEmpty()) {
        sqlScriptSuffixes.add('.sql')
    }

    def dbDialect = env('db.dialect')
    if (dbDialect) {
        def colonIndex = dbDialect.indexOf(':')
        if (colonIndex > -1) {
            dbDialect = dbDialect.substring(0, colonIndex)
        }
    }

    sqlScriptPrefixes.each { sqlScriptPrefix ->

        sqlScriptSuffixes.each { sqlScriptSuffix ->

            final sqlScriptLocation =
                absolutePath(dbDialect, "${sqlScriptPrefix}${dbDialect ? '.' + dbDialect : ''}${sqlScriptSuffix}")
            log.info("Executing script ${sqlScriptLocation}${dry ? ' (dry run)' : '...'}")

            if (!dry) {
                try {
                    executeSqlScript(dataSource, config, sqlScriptLocation)
                } catch (ex) {
                    handle(ex, { "An error occurred while executing script ${sqlScriptLocation}" })
                }
            }
        }
    }
}

private executeSqlScript(DataSource dataSource, FlywayConfiguration config, String sqlScriptLocation) {
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
    switch (env('flyway.logLevel')) {
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
    fallbackLogCreator = new ConsoleLogCreator(level)
    return getLog(getClass())
}

private FlywayConfiguration loadFlywayConfig() {
    final log = getLog(getClass())
    final config = [:]
    final cfgEnc = env(CONFIG_FILE_ENCODING, 'UTF-8')
    final loadOpt = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, false)
    final loadMnd = ConfigUtils.&loadConfigurationFile.rcurry(cfgEnc, true)

    // Defaults:
    config.put(LOCATIONS,
               env(LOCATIONS, { FILESYSTEM_PREFIX + absolutePath(env('db.dialect'), 'migration', Files.&isDirectory) }))

    // Configuration files:
    config.putAll(loadOpt(new File(absolutePath('conf', CONFIG_FILE_NAME))))
    config.putAll(loadOpt(new File(properties['user.home'] as String, CONFIG_FILE_NAME)))
    config.putAll(loadOpt(new File(CONFIG_FILE_NAME)))

    def configFiles = env(CONFIG_FILE)
    if (configFiles) {
        log.warn('configFile is deprecated and will be removed in Flyway 6.0. Use configFiles instead.')
        config.putAll(loadMnd(new File(configFiles)))
    }
    configFiles = env(CONFIG_FILES)
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
            def val = env(key, '')
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
    final flyway = new Flyway()
    flyway.configure(config)
    return flyway
}

private String absolutePath(String parent = '', String child = '',
                            Closure<?> test = Files.&isRegularFile,
                            String basedir = env('basedir', { Paths.get('') as String })) {

    def path = Paths.get(basedir, parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    final build = env('build') // Maven?

    final outDir = build?.hasProperty('outputDirectory')?.getProperty(build) as String ?: 'target/classes'
    path = Paths.get(basedir, outDir, parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    final pkgDir = env('project.build.package', { getClass().package?.name?.replace('.', '/') })
    path = Paths.get(basedir, pkgDir, parent, child) // under package / namespace?
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    path = Paths.get(basedir, outDir, pkgDir, parent, child)
    if (test(path)) {
        return path.toAbsolutePath().normalize() as String
    }

    return Paths.get(basedir, parent, child).toAbsolutePath().normalize() as String
}

private String env(String name, def defaultOrProvider = '') {
    // Maven / Gradle project passed in bindings?
    final project = binding.variables.project

    ((System.properties?."$name" ?: //@fmt:off
      project?.properties?.get(name)                   ?:
      project?.hasProperty(name)?.getProperty(project) ?:
      System.getenv(name)                              ?:
      binding.variables?."$name"                       ?: //@fmt:on
      (defaultOrProvider instanceof Closure ? defaultOrProvider.call() : defaultOrProvider) ?:
      '') as String).trim()
}

private handle(Exception exception, def messageOrProvider = '') {
    if (exception) {
        final log = getLog(getClass())
        exception instanceof FlywayException ? log.error(exception.getMessage()) : log.error(
            (messageOrProvider && messageOrProvider instanceof Closure ? messageOrProvider.call() :
             messageOrProvider) as String, exception)
    }
}

enum OptValue {

    FORCE('f', 'force'), YES('y', 'yes')

    private final synonyms = []

    OptValue(String... synonyms) {
        if (synonyms) {
            this.synonyms.addAll(synonyms)
        }
    }

    @Override
    String toString() { synonyms.join('|') }

    static String opts(OptValue... values) { values?.join('|') ?: '' }

    boolean eq(def value) {
        value && (value instanceof OptValue ? equals(value) : synonyms.contains((value as String).toLowerCase()))
    }
}
