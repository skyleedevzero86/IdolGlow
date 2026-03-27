plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.4"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("kapt") version "2.2.21"
}

group = "com.sleekydz86"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val querydslDir = "src/main/generated"

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-aop:4.0.0-M2")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")

	//DB
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.mysql:mysql-connector-j")

	//mockk
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation("io.mockk:mockk:1.13.8")

	//kotest
	val kotestVersion = "5.5.4"
	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.rest-assured:rest-assured:5.4.0")

	//Jsypt
	implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")

	// Spring Data jpa
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	//webclient
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("io.netty:netty-resolver-dns-native-macos:4.1.123.Final:osx-aarch_64")

	// QueryDsl
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
	kapt("jakarta.annotation:jakarta.annotation-api")
	kapt("jakarta.persistence:jakarta.persistence-api")

	//JWT
	compileOnly("io.jsonwebtoken:jjwt-api:0.11.2")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

	// SWAGGER
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
	}
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
	options.release.set(24)
}

kapt {
	arguments {
		arg("querydsl.sourcesDir", querydslDir)
	}
}

sourceSets {
	main {
		kotlin.srcDir(querydslDir)
	}
}

tasks.named("clean") {
	doLast {
		file(querydslDir).deleteRecursively()
	}
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	jvmArgs("--enable-native-access=ALL-UNNAMED")
}