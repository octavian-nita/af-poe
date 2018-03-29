package net.appfold.poe.db

import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator

import static org.flywaydb.core.api.logging.LogFactory.setFallbackLogCreator
import static org.flywaydb.core.internal.util.ClassUtils.getLocationOnDisk
import static org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level.*

println new File(getLocationOnDisk(getClass())).parentFile.parentFile.absolutePath

switch (cmd = env('db.cmd').toLowerCase()) {

case 'migrate':
case 'create':
case 'drop':
    "$cmd"()
    break

case '':
    migrate()
    break

default:
    throw new RuntimeException("Database control command '${cmd}' not supported!")
}

def migrate() { println 'migrate...' }

def create() { println 'create...' }

def drop() {
    def confirm = env('db.confirmDrop').toLowerCase()
    def final yes = ['y', 'yes', 'true']

    if (!(confirm in yes)) {
        println ''
        println '!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println '!!                                                          !!'
        println '!! Serious data loss might occur when dropping the database !!'
        println '!!                                                          !!'
        println '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        println ''

        def console = System.console()
        if (console) {
            confirm = console.readLine('Drop the database?  [y/N]: ').trim().toLowerCase()
        }
    }

    if (!(confirm in yes)) {
        println "Use -Ddb.confirmDrop=[${yes.join('|')}] to confirm database dropping"
        return 1
    }

    withDbAdminCredentials { username, password ->
        println "${username}:${password}"
    }
}

private env(String name, String defaultValue = '') {
    ((System.properties[name] ?: System.getenv(name) ?: binding.variables[name] ?:
                                                        binding.variables.project?.properties?.get(name) ?:
                                                        binding.variables.project?."${name}" ?:
                                                        defaultValue) as String).trim()
}

private withDbAdminCredentials(callback) {
    String username = env('db.adminUsername')
    char[] password = env('db.adminPassword').getBytes()

    def console = System.console()
    if (console) {
        if (!username) {
            username = console.readLine('DB admin username [admin]: ').trim()
            if (!username) {
                username = 'admin'
            }
        }
        if (!password) {
            password = console.readPassword('DB admin password: ')
        }
    }

    try {
        callback(username, password)
    } finally {
        if (password) {
            Arrays.fill(password, ' ' as char)
        }
    }
}

private Properties loadFlywayConfig() {
    def final properties = new Properties()

    def flywayEnv = ConfigUtils.environmentVariablesToPropertyMap()

    def final basedir = new File(getLocationOnDisk(getClass())).parentFile.parentFile.absolutePath

    //def workDir = env('')  workingDirectory == null ? mavenProject.getBasedir() : workingDirectory;

    properties.put(ConfigUtils.LOCATIONS, "filesystem:" + new File(getInstallationDir(), "sql").getAbsolutePath());
    properties.put(ConfigUtils.JAR_DIRS, new File(getInstallationDir(), "jars").getAbsolutePath());

    loadConfigurationFromConfigFiles(properties, args, envVars);
    properties.putAll(envVars);
    overrideConfigurationWithArgs(properties, args);

    filterProperties(properties);

    properties
}

private initFlywayLogging() {
    def level
    switch (env('flyway.logLevel')) {
    case '-X':
        level = DEBUG
        break
    case '-q':
        level = WARN
        break
    default:
        level = INFO
        break
    }
    setFallbackLogCreator(new ConsoleLogCreator(level))
    LogFactory.getLog(getClass());
}
