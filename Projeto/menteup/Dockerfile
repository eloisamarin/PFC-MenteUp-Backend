# ==============================================================================
# Dockerfile do backend MenteUp (Spring Boot)
#
# Usa "multi-stage build" (construção em duas etapas):
#   1ª etapa (build): compila o projeto com Maven e gera o .jar;
#   2ª etapa (final): monta a imagem que vai rodar, só com o que é necessário.
# Assim, o Maven, o código-fonte e os arquivos temporários da compilação
# não vão para a imagem final, que fica menor e mais segura.
#
# Para construir:  docker build -t menteup .
# Para rodar:      docker run -p 8080:8080 -e DATABASE_URL=... -e JWT_SECRET=... menteup
# ==============================================================================


# ------------------------------------------------------------------------------
# 1ª ETAPA: compilar o projeto
# ------------------------------------------------------------------------------

# Imagem base com o JDK 21 (Java completo, com compilador) da distribuição Eclipse Temurin.
# Precisa ser a mesma versão do pom.xml (<java.version>21</java.version>).
# "AS build" dá um nome a esta etapa, para a 2ª etapa poder copiar arquivos dela.
FROM eclipse-temurin:21-jdk AS build

# Define a pasta de trabalho dentro do container. Os comandos seguintes rodam nela
# (se a pasta não existir, o Docker cria).
WORKDIR /workspace/app

# Copia o Maven Wrapper (script que baixa e roda a versão certa do Maven,
# sem precisar ter o Maven instalado na imagem).
COPY mvnw .

# Copia a pasta de configuração do Maven Wrapper (indica qual versão do Maven usar).
COPY .mvn .mvn

# Copia o pom.xml, que lista as dependências e como o projeto é construído.
COPY pom.xml .

# Copia o código-fonte (Java, application.properties e testes).
COPY src src

# Prepara o mvnw para rodar no Linux do container:
#   sed  -> tira o "\r" das quebras de linha do Windows (sem isso dá "/bin/sh^M: bad interpreter");
#   chmod +x -> dá permissão de execução só ao que precisa (mais seguro que 777).
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Compila o projeto e gera o .jar em target/, pulando os testes (-DskipTests)
# para a imagem ser construída mais rápido. Os testes rodam na pipeline (CI).
# "package" gera o .jar (o "install" faria o mesmo e ainda copiaria para o repositório
# local do Maven, o que não é necessário aqui).
RUN ./mvnw package -DskipTests

# Cria a pasta target/dependency e "desmonta" o .jar dentro dela (jar -xf = extrair).
# Separar o .jar em partes (bibliotecas, configurações e classes do projeto) deixa a
# imagem final mais eficiente: as bibliotecas quase nunca mudam e ficam em cache no Docker.
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)


# ------------------------------------------------------------------------------
# 2ª ETAPA: imagem final, que roda a aplicação
# ------------------------------------------------------------------------------

# Nova imagem base, começando do zero: nada da 1ª etapa vem junto, só o que for copiado.
# Usa o JRE 21 (só o necessário para rodar Java, sem compilador): imagem bem menor que a do JDK.
FROM eclipse-temurin:21-jre

# Cria um volume em /tmp. O Spring Boot (Tomcat embutido) grava arquivos temporários ali;
# com o volume, eles ficam fora da camada do container.
VOLUME /tmp

# Variável usada só durante a construção da imagem: o caminho onde o .jar foi
# desmontado na 1ª etapa. Evita repetir esse caminho nas linhas abaixo.
ARG DEPENDENCY=/workspace/app/target/dependency

# Copia da 1ª etapa (--from=build) as bibliotecas do projeto (Spring, Hibernate, JWT...)
# para /app/lib. Ficam numa camada própria, reaproveitada enquanto o pom.xml não mudar.
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib

# Copia os metadados do .jar (versão, informações do Maven) para /app/META-INF.
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF

# Copia as classes compiladas do MenteUp e os arquivos de recursos
# (application.properties, templates) para /app. É a camada que muda a cada alteração no código.
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Informa que a aplicação escuta na porta 8080 (server.port do application.properties).
# Para acessar de fora, publique a porta ao rodar: docker run -p 8080:8080 ...
EXPOSE 8080

# Comando executado quando o container inicia:
#   java                                  -> roda a Máquina Virtual Java;
#   -cp app:app/lib/*                     -> classpath: as classes do projeto (app) e todas as bibliotecas (app/lib);
#   com.umc.menteup.MenteupApplication    -> a classe com o main() que sobe o Spring Boot.
# As senhas e chaves NÃO ficam na imagem: passe as variáveis de ambiente ao rodar o container
# (DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, EMAIL_PROJETO, SENHA_EMAIL, JWT_SECRET...).
ENTRYPOINT ["java","-cp","app:app/lib/*","com.umc.menteup.MenteupApplication"]
