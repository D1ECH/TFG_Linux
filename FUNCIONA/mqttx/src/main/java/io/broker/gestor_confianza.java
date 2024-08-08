package io.broker;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import io.broker.Dispositivos.Dispositivo;

public class gestor_confianza {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Variables globales
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    final static String DB_URL = "jdbc:mysql://localhost/trust_management";
    final static String USERNAME = "root";
    final static String PASSWORD = "";

    static List<Dispositivo> operatorThings;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Tratamiento de ID y anomalía
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String ID_existente(String mensajeCliente, List<Dispositivo> operatorThings) {
        String res = "No se ha encontrado el dispositivo entre los existentes";

        // Imprimir el argumento recibido
        System.out.println("\nArgumento recibido: " + mensajeCliente);

        // Decodificar el argumento Base64
        byte[] decodedBytes = Base64.getDecoder().decode(mensajeCliente);
        String decodedJson = new String(decodedBytes);

        // Imprimir el JSON decodificado
        System.out.println("\nJSON decodificado: " + decodedJson + "\n");

        // Crear una instancia de Gson
        Gson gson = new Gson();

        try {
            // Limpiar el JSON decodificado
            String cleanJson = decodedJson.trim().replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

            // Usar JsonReader en modo lenient para permitir formato flexible
            JsonReader reader = new JsonReader(new StringReader(cleanJson));
            reader.setLenient(true);

            // Convertir la cadena JSON en un objeto JsonObject
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            // Extraer los valores del JSON
            String idAnomalia = jsonObject.get("anomaliaID").getAsString();
            String amenaza = jsonObject.get("amenaza").getAsString();
            String thingID = jsonObject.get("thingID").getAsString();

            // Verificar si el JSON contiene los campos antiguos o el nuevo campo de nivel de riesgo
            if (jsonObject.has("probabilidad") && jsonObject.has("gravedad") && jsonObject.has("detectabilidad")) {
                String probabilidad = jsonObject.get("probabilidad").getAsString();
                String gravedad = jsonObject.get("gravedad").getAsString();
                String detectabilidad = jsonObject.get("detectabilidad").getAsString();

                // Imprimir los valores obtenidos
                System.out.println(" # ID de Anomalía: " + idAnomalia);
                System.out.println(" # Amenaza: " + amenaza);
                System.out.println(" # thingID: " + thingID);
                System.out.println(" # Probabilidad: " + probabilidad);
                System.out.println(" # Gravedad: " + gravedad);
                System.out.println(" # Detectabilidad: " + detectabilidad);

                if (encontrarBD(thingID)) {
                    res = "Dispositivo encontrado";
                    res = nivel_riesgo(probabilidad, gravedad, detectabilidad, thingID);
                }
            } else if (jsonObject.has("nivelRiesgo")) {
                String nivelRiesgo = jsonObject.get("nivelRiesgo").getAsString();

                // Imprimir los valores obtenidos
                System.out.println(" # ID de Anomalía: " + idAnomalia);
                System.out.println(" # Amenaza: " + amenaza);
                System.out.println(" # thingID: " + thingID);
                System.out.println(" # Nivel de Riesgo: " + nivelRiesgo);

                if (encontrarBD(thingID)) {
                    res = "Dispositivo encontrado";
                    actualizarEstadoBD(nivelRiesgo, thingID);
                }
            } else {
                res = "Error: JSON no contiene los campos esperados.";
                System.out.println(res);
            }
        } catch (JsonSyntaxException e) {
            // Capturar cualquier error de sintaxis en el JSON
            res = "Error al parsear JSON: " + e.getLocalizedMessage();
            System.out.println(res);
        }

        return res;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    * Cáculo del nivel de riesgo en base a los datos de la anomalía: criticidad, probabilidad y detectabilidad.
    * Los valores se combinan entre ellos mediante una multiplicación. Este es un enfoque común utilizado 
    * en muchos métodos de riesgo [2]. Si el resultado es inferior a 9, tenemos un riesgo bajo. Si el resultado 
    * está entre 9 y 27, tenemos un riesgo medio. Si el valor es superior a 27, tenemos un riesgo alto. El valor del riesgo 
    * global se ha elegido según los siguientes criterios:
    * 
    *  1) Es el mismo nivel de todos los parámetros si pertenecen al mismo nivel (es decir, bajo si L, S y D son bajos).
    *  2) Bajo, si solo hay un parámetro medio y los otros dos parámetros son bajos.
    *  3) Alto, si hay dos o más parámetros configurados en alto o dos parámetros configurados en medio y uno configurado en alto.
    *  4) Medio, en caso contrario.
    * En el caso de que el riesgo calculado sea alto, el dispositivo no se podrá agregar a la red o se deberá prohibir. 
    * En el caso de que el valor del riesgo sea bajo o medio el dispositivo puede unirse o permanecer en la red dependiendo de otros criterios
    */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String nivel_riesgo(String probabilidad, String gravedad, String detectabilidad, String thingID){
        //Almacenamos el estado actual tras haberlo recuperado de la BD;
        int estado_actual=0;
    
        int calculoRiesgo = Integer.parseInt(probabilidad) * Integer.parseInt(gravedad) * Integer.parseInt(detectabilidad);
        if(calculoRiesgo > 27){
            //El riesgo es alto, el dispositivo debe prohibirse, no podrá ser agregado en la red.
            //Localizar el dispositivo por su ID --> thingID y actualizar su estado a PROHIBIDO
            System.out.println("Estado: PROHIBIDO - Nivel de riesgo: " + calculoRiesgo);
            estado_actual = 3;
        }else if(calculoRiesgo <= 27 && calculoRiesgo >= 9){
            //El riesgo es medio, el dispositivo puede unirse/permanecer en la red siempre y cuando el nivel de CONFIANZA cumple los requisitos mínimos
            System.out.println("Estado: PENDIENTE DE EVALUACIÓN - Nivel de riesgo: " + calculoRiesgo);
            estado_actual = 2;
        }else if(calculoRiesgo < 9){
            //El riesgo es bajo, el dispositivo puede unirse/permanecer en la red siempre y cuando el nivel de CONFIANZA cumple los requisitos mínimos
            System.out.println("Estado: PENDIENTE DE EVALUACIÓN - Nivel de riesgo: " + calculoRiesgo);
            estado_actual = 1;
        }
    
        
        
        /*
        * Actualización del estado del dispositivo en la base de datos: si se prohibe debe borrarse de la BD para que en caso de que se mande más
            información del dispositivo directamente ni exista en el sistema. Lo cual NO ES LO MISMO que estar en cuarentena, donde no puede comunicarse,
            pero sigue en la red.
        */
        // Determinar el estado del dispositivo
        String estado = null;
        if (estado_actual == 3) {
            estado = "PROHIBIDO";
            actualizarEstadoBD(thingID, estado);

        } else if (estado_actual == 2 || estado_actual == 1) {
            // Recuperar información del dispositivo de la BD y crear un Dispositivo para usar el método de calcular_confianza
            Dispositivo dispositivo = recuperarDispositivoDeBD(Integer.parseInt(thingID));

            if (dispositivo != null) {
                double confianza = dispositivo.calcularConfianza();
                System.out.println("Confianza del dispositivo con ID " + thingID + ": " + confianza);

                // Determinar el estado basado en la confianza calculada
                if (confianza < 0.3) {
                    estado = "CUARENTENA";
                } else if (confianza >= 0.3 && confianza < 0.7) {
                    estado = "PENDIENTE_EVALUACION";
                    // Aquí se puede añadir lógica para más comprobaciones si el estado es pendiente de evaluación
                    // Por ejemplo, se podría realizar una revisión manual o enviar una alerta para una inspección más detallada
                } else if (confianza >= 0.7) {
                    estado = "ACEPTADO";
                }

                // Actualizar el estado en la base de datos
                actualizarEstadoBD(thingID, estado);

                // Recomputar y actualizar la reputación en la BD
                recomputarYActualizarReputacion(Integer.parseInt(thingID), confianza);
                actualizarReputacion(Integer.parseInt(thingID), confianza);
                System.out.println("Confianza y reputación readys");
            } else {
                System.out.println("No se encontró el dispositivo con ID " + thingID);
            }
        }
        
        
        /*
        * Actualización del estado del dispositivo en la base de datos: si queda en cuarentena deben terminar sus comunicaciones desubscribiendo sus topics,
            borrando los topics...
        */
        //actualizarEstadoBD(thingID, estado);


        /*
        * Actualización reputación
        */
        

        // /*
        // * Comunicar el estado al resto de dispositivos
        // */
        
        String r = "Ha llegado hasta aquí y el nivel de riesgo es: " + estado_actual + "\n";
        return r;//estado_actual;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Buscar un ID de dispositivo en la BD de información de dispositivos
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Boolean encontrarBD(String thingID){
        boolean res = false;
        // Quizás habría que buscar antes los dispositivos en la BD y a partir de ahí ya comparar con los datos obtenidos ?????
        Connection conn = null;

        try {
            // CONEXIÓN A LA BASE DE DATOS
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            //System.out.println("Connected database successfully...");
            

            // Consulta para verificar si el dispositivo existe
            String query = "SELECT * FROM DISPOSITIVOS_INFO WHERE ID_DISPOSITIVO = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, Integer.parseInt(thingID));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Dispositivo encontrado, calcular el nivel de riesgo
                System.out.println("\nEl dispositivo se encuentra entre los existentes.");
                res = true;
            
            } else {
                // Dispositivo no encontrado
                System.out.println("\nEl dispositivo no se encuentra en la base de datos.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return res;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Actualizar el estado de un ID de dispositivo en la BD de información de dispositivos
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void actualizarEstadoBD(String thingID, String nuevoEstado){
        Connection conn = null;

        try {
            // CONEXIÓN A LA BASE DE DATOS
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            //System.out.println("Connected database successfully...");
            

            String updateQuery = "UPDATE DISPOSITIVOS_INFO SET ESTADO_ACTUAL = ? WHERE ID_DISPOSITIVO = ?";
            PreparedStatement pstmt = null;

            pstmt = conn.prepareStatement(updateQuery);
            pstmt.setString(1, nuevoEstado);
            pstmt.setInt(2, Integer.parseInt(thingID));

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("\nEstado del dispositivo actualizado correctamente.");
            } else {
                System.out.println("\nNo se encontró el dispositivo con el ID especificado.");
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Actualizar la reputación de un dispositivo en la BD de reputación
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void actualizarReputacion(int dispositivoID, double confianza) {
        Connection conn = null;

        try {
            // CONEXIÓN A LA BASE DE DATOS
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            //System.out.println("Connected database successfully...");
            
        
            String updateQuery = "UPDATE REPUTACION SET VALOR_REPUTACION = ? WHERE ID_DISPOSITIVO = ?";
            PreparedStatement pstmt = null;

        
            pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, confianza);
            pstmt.setInt(2, dispositivoID);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("\nReputación del dispositivo actualizada correctamente.");
            } else {
                System.out.println("\nNo se encontró el dispositivo con el ID especificado.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Recuperar dispositivo de la BD
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Dispositivo recuperarDispositivoDeBD(int id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Conectar a la base de datos
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

            // Preparar la consulta SQL
            String sql = "SELECT * FROM DISPOSITIVOS_INFO WHERE ID_DISPOSITIVO = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);

            // Ejecutar la consulta
            rs = pstmt.executeQuery();

            // Procesar el resultado
            if (rs.next()) {
                int dispositivoID = rs.getInt("ID_DISPOSITIVO");
                LocalDate lastSupervision = rs.getDate("LAST_SUPERVISION").toLocalDate();
                int numPastVulns = rs.getInt("NUM_PAST_VULNS");
                boolean securityCertification = rs.getBoolean("SECURITY_CERTIFICATION");
                int reputacionInicial = rs.getInt("REPUTACION");
                int tiempoFuncionamiento = rs.getInt("TIEMPO_FUNCIONAMIENTO");
                int actualizacionesFirmware = rs.getInt("ACTUALIZACIONES_FIRMWARE");
                int frecuenciaComunicacion = rs.getInt("FRECUENCIA_COMUNICACION");
                int historialFallos = rs.getInt("HISTORIAL_FALLOS");
                String tipo = rs.getString("TIPO");
                String estado = rs.getString("ESTADO_ACTUAL");

                // Crear y devolver el objeto Dispositivo
                Dispositivo dispositivo = new Dispositivo(dispositivoID, numPastVulns, securityCertification, lastSupervision, 
                                                        tiempoFuncionamiento, actualizacionesFirmware, frecuenciaComunicacion, historialFallos, reputacionInicial, tipo, estado);
                return dispositivo;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;  // Retornar null si no se encuentra el dispositivo
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Recomputar reputación y actualizarla en la BD de dispositivos.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void recomputarYActualizarReputacion(int id, double confianzaCalculada) {
        Connection conn = null;
        PreparedStatement pstmt = null;
    
        try {
            // Conectar a la base de datos
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
    
            // Recuperar la reputación inicial del dispositivo
            String selectSQL = "SELECT REPUTACION FROM DISPOSITIVOS_INFO WHERE ID_DISPOSITIVO = ?";
            pstmt = conn.prepareStatement(selectSQL);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                int reputacionInicial = rs.getInt("REPUTACION");
                int nuevaReputacion;
    
                // Recomputar la reputación en función de la confianza calculada
                if (confianzaCalculada >= 0.8) {
                    nuevaReputacion = reputacionInicial + 20; // Incremento significativo
                } else if (confianzaCalculada >= 0.5) {
                    nuevaReputacion = reputacionInicial + 10; // Incremento moderado
                } else {
                    nuevaReputacion = reputacionInicial - 15; // Reducción
                }
    
                // Asegurarse de que la reputación esté entre 0 y 100
                if (nuevaReputacion > 100) {
                    nuevaReputacion = 100;
                } else if (nuevaReputacion < 0) {
                    nuevaReputacion = 0;
                }
    
                // Actualizar la reputación en la base de datos
                String updateSQL = "UPDATE DISPOSITIVOS_INFO SET REPUTACION = ? WHERE ID_DISPOSITIVO = ?";
                pstmt = conn.prepareStatement(updateSQL);
                pstmt.setInt(1, nuevaReputacion);
                pstmt.setInt(2, id);
    
                pstmt.executeUpdate();
                System.out.println("\nReputación actualizada correctamente para el dispositivo con ID " + id);
            } else {
                System.out.println("\nNo se encontró el dispositivo con ID " + id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    





















    










    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MAIN
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) throws Exception {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // GENERACIÓN DISPOSITIVOS
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ArrayList<Dispositivo> operatorThings = new ArrayList<>();
        Dispositivo thingRandom;

        // // Generación de Dispositivos --> Se generan 4 con IDs random
        // for (int j = 0; j < 4; j++) {
        //     // thingID: 0 to 32000
        //     SecureRandom randomThingID = new SecureRandom();
        //     int thingID = randomThingID.nextInt(32001);

        //     // LastSupervision: 01/01/2017 to 31/12/2017 
        //     long minDayThing = LocalDate.of(2017, 1, 1).toEpochDay();
        //     long maxDayThing = LocalDate.of(2017, 12, 31).toEpochDay();
        //     long randomDayThing = ThreadLocalRandom.current().nextLong(minDayThing, maxDayThing);
        //     LocalDate lastSupervision = LocalDate.ofEpochDay(randomDayThing);

        //     // NbPastDefaillances: 0 to 30
        //     SecureRandom randomDefaillances = new SecureRandom();
        //     int nbPastDefaillances = randomDefaillances.nextInt(31);

        //     // SecurityCertification: True or False
        //     SecureRandom randomCertification = new SecureRandom();
        //     boolean securityCertification = randomCertification.nextBoolean();

        //     // Tipo de dispositivo: Físico || Virtual
        //     String tipo = (randomCertification.nextBoolean()) ? "Fisico" : "Virtual";

        //     // Reputación inicial: 0 a 100
        //     int reputacionInicial = randomThingID.nextInt(101);

        //     // Estado actual: "pendiente_evaluacion"
        //     String estadoActual = "pendiente_evaluacion";

        //     // Tiempo de funcionamiento: 0 a 2000 horas
        //     int tiempoFuncionamiento = randomThingID.nextInt(2001);

        //     // Actualizaciones de firmware: 0 a 20
        //     int actualizacionesFirmware = randomThingID.nextInt(21);

        //     // Frecuencia de comunicación: 0 a 200
        //     int frecuenciaComunicacion = randomThingID.nextInt(201);

        //     // Historial de fallos: 0 a 10
        //     int historialFallos = randomThingID.nextInt(11);

        //     // Constructor de Dispositivo
        //     thingRandom = new Dispositivo(thingID, nbPastDefaillances, securityCertification, lastSupervision, 
        //                                   tiempoFuncionamiento, actualizacionesFirmware, frecuenciaComunicacion, historialFallos,
        //                                   reputacionInicial, tipo, estadoActual);
        //     operatorThings.add(thingRandom);
        // }

        // // Dispositivo no aleatorio para que coincida con el creado en el cliente
        // Dispositivo dispositivoCoincide = new Dispositivo(1, 1, true, LocalDate.of(2002, 1, 1), 
        //                                                   0, 0, 0, 0, 1, "virtual", "pendiente_evaluacion");
        // operatorThings.add(dispositivoCoincide);




        
        // Generación de Dispositivos según los casos de estudio
        // Caso 1: Dispositivo no existe (error)
        // Este caso será manejado en tiempo de ejecución, no se necesita generar un dispositivo específico

        // Caso 2: Dispositivo nuevo, historial de reputación nulo, quedará prohibido
        Dispositivo dispositivoProhibido = new Dispositivo(1, 0, false, LocalDate.of(2022, 1, 1),
                0, 0, 0, 0, 0, "virtual", "pendiente_evaluacion");
        operatorThings.add(dispositivoProhibido);

        // Caso 3: Dispositivo nuevo, historial de reputación nulo, quedará en cuarentena
        Dispositivo dispositivoCuarentena = new Dispositivo(2, 0, true, LocalDate.of(2022, 1, 1),
                0, 0, 0, 0, 0, "fisico", "pendiente_evaluacion");
        operatorThings.add(dispositivoCuarentena);

        // Caso 4: Dispositivo nuevo, historial de reputación nulo, quedará incluido en la red
        Dispositivo dispositivoIncluido = new Dispositivo(3, 0, true, LocalDate.of(2022, 1, 1),
                0, 0, 0, 0, 0, "virtual", "pendiente_evaluacion");
        operatorThings.add(dispositivoIncluido);

        // Caso 5: Dispositivo con historial, quedará prohibido
        Dispositivo dispositivoHistorialProhibido = new Dispositivo(4, 10, true, LocalDate.of(2020, 1, 1),
                100, 5, 50, 3, 20, "fisico", "pendiente_evaluacion");
        operatorThings.add(dispositivoHistorialProhibido);

        // Caso 6: Dispositivo con historial, quedará en cuarentena
        Dispositivo dispositivoHistorialCuarentena = new Dispositivo(5, 5, true, LocalDate.of(2019, 1, 1),
                200, 3, 30, 2, 50, "virtual", "pendiente_evaluacion");
        operatorThings.add(dispositivoHistorialCuarentena);

        // Caso 7: Dispositivo con historial, quedará incluido en la red
        Dispositivo dispositivoHistorialIncluido = new Dispositivo(6, 2, true, LocalDate.of(2018, 1, 1),
                300, 10, 70, 1, 80, "fisico", "pendiente_evaluacion");
        operatorThings.add(dispositivoHistorialIncluido);
        
        // Caso 8: Dispositivo nuevo, nivel de riesgo muy alto, quedará prohibido
        Dispositivo dispositivoRiesgoAltoProhibido = new Dispositivo(7, 0, false, LocalDate.of(2023, 1, 1),
                0, 0, 0, 0, 0, "virtual", "pendiente_evaluacion");
        operatorThings.add(dispositivoRiesgoAltoProhibido);

        // Caso 9: Dispositivo nuevo, nivel de riesgo medio, quedará en cuarentena
        Dispositivo dispositivoRiesgoMedioCuarentena = new Dispositivo(8, 0, true, LocalDate.of(2023, 1, 1),
                0, 0, 0, 0, 0, "fisico", "pendiente_evaluacion");
        operatorThings.add(dispositivoRiesgoMedioCuarentena);

        // Caso 10: Dispositivo nuevo, nivel de riesgo bajo, quedará incluido en la red
        Dispositivo dispositivoRiesgoBajoIncluido = new Dispositivo(9, 0, true, LocalDate.of(2023, 1, 1),
                0, 0, 0, 0, 0, "virtual", "pendiente_evaluacion");
        operatorThings.add(dispositivoRiesgoBajoIncluido);

        // // TRAZA
        // // Mostramos los IDs de los dispositivos creados
        // for (Dispositivo dispositivo : operatorThings) {
        //     System.out.println(dispositivo);
        // }

        // System.out.println("\n");
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // CONEXIÓN A BD Y CREAR TABLAS DISPOSITIVOS Y REPUTACIÓN
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Connection conn = null;
        Statement stmt = null;

        try {
            // CONEXIÓN A LA BASE DE DATOS
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            System.out.println("Connected database successfully...");

            // Verificar si la tabla ya existe
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet tables = metadata.getTables(null, null, "DISPOSITIVOS_INFO", null);

            if (!tables.next()) {
                // La tabla no existe, por lo tanto, podemos proceder a crearla

                // CREAR STATEMENT
                stmt = conn.createStatement();

                // CREACIÓN DE LA TABLA
                String sql = "CREATE TABLE DISPOSITIVOS_INFO " +
                        "(ID_DISPOSITIVO INTEGER not NULL, " +
                        " LAST_SUPERVISION DATE, " + 
                        " NUM_PAST_VULNS INTEGER, " + 
                        " SECURITY_CERTIFICATION BOOLEAN, " +
                        " TIPO VARCHAR(128), " +
                        " REPUTACION INTEGER, "+ 
                        " ESTADO_ACTUAL VARCHAR(128), " +
                        " TIEMPO_FUNCIONAMIENTO INTEGER, " +
                        " ACTUALIZACIONES_FIRMWARE INTEGER, " +
                        " FRECUENCIA_COMUNICACION INTEGER, " +
                        " HISTORIAL_FALLOS INTEGER, " +
                        " PRIMARY KEY ( ID_DISPOSITIVO ))"; 

                stmt.executeUpdate(sql);
                System.out.println("Created table in given database..."); 

                ///////////////////////////////////////////////// INSERTAR INFORMACIÓN EN LA TABLA ////////////////////////////////////////////////////////////////////////////////////////
                String insertarSQL = "INSERT INTO DISPOSITIVOS_INFO (ID_DISPOSITIVO, LAST_SUPERVISION, NUM_PAST_VULNS, SECURITY_CERTIFICATION, TIPO, REPUTACION, ESTADO_ACTUAL, " +
                "TIEMPO_FUNCIONAMIENTO, ACTUALIZACIONES_FIRMWARE, FRECUENCIA_COMUNICACION, HISTORIAL_FALLOS) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstmt = conn.prepareStatement(insertarSQL);

                for (Dispositivo dispositivo : operatorThings) {
                pstmt.setInt(1, dispositivo.getId());
                pstmt.setDate(2, java.sql.Date.valueOf(dispositivo.getUltimaRevision()));
                pstmt.setInt(3, dispositivo.getNumVulnerabilidades());
                pstmt.setBoolean(4, dispositivo.isTieneCertificado());
                pstmt.setString(5, dispositivo.getTipo());
                pstmt.setInt(6, dispositivo.getReputacionInicial());
                pstmt.setString(7, dispositivo.getEstadoActual());
                pstmt.setInt(8, dispositivo.getTiempoFuncionamiento());
                pstmt.setInt(9, dispositivo.getActualizacionesFirmware());
                pstmt.setInt(10, dispositivo.getFrecuenciaComunicacion());
                pstmt.setInt(11, dispositivo.getHistorialFallos());

                pstmt.executeUpdate();
                }

                System.out.println("Datos insertados correctamente...");
            } else {
                System.out.println("Table already exists in the database.");
            }

            // Verificar si la tabla REPUTACION ya existe
            ResultSet tablesReputacion = metadata.getTables(null, null, "REPUTACION", null);

            if (!tablesReputacion.next()) {
                // La tabla REPUTACION no existe, por lo tanto, podemos proceder a crearla

                // CREACIÓN DE LA TABLA REPUTACION
                String sqlReputacion = "CREATE TABLE REPUTACION " +
                "(ID_HISTORICO INT AUTO_INCREMENT PRIMARY KEY not NULL, " +
                " ID_DISPOSITIVO INTEGER not NULL, " + 
                " TIMESTAMP VARCHAR(128) not NULL, " + 
                " VALOR_REPUTACION INTEGER)"; 

                stmt.executeUpdate(sqlReputacion);
                System.out.println("Created REPUTACION table in the database..."); 

                // Insertar datos de reputación preexistentes para dispositivos con historial
                Map<Integer, Integer> reputacionesPreexistentes = new HashMap<>();
                reputacionesPreexistentes.put(4, 20); // Dispositivo 4 con reputación 20
                reputacionesPreexistentes.put(5, 50); // Dispositivo 5 con reputación 50
                reputacionesPreexistentes.put(6, 80); // Dispositivo 6 con reputación 80

                String insertarReputacionSQL = "INSERT INTO REPUTACION (ID_DISPOSITIVO, TIMESTAMP, VALOR_REPUTACION) " +
                        "VALUES (?, ?, ?)";
                PreparedStatement pstmtReputacion = conn.prepareStatement(insertarReputacionSQL);

                for (Map.Entry<Integer, Integer> entry : reputacionesPreexistentes.entrySet()) {
                    int dispositivoID = entry.getKey();
                    int reputacion = entry.getValue();

                    // Generar un timestamp actual en Java
                    long timestampMillis = System.currentTimeMillis();
                    Timestamp timestamp = new Timestamp(timestampMillis);

                    pstmtReputacion.setInt(1, dispositivoID);
                    pstmtReputacion.setString(2, timestamp.toString());
                    pstmtReputacion.setInt(3, reputacion);

                    pstmtReputacion.executeUpdate();
                }

                System.out.println("Datos de reputación preexistentes insertados correctamente...");

                ///////////////////////////////////////////////// RECUPERAR LOS VALORES MÁS ACTUALIZADOS DE LA TABLA DE DISPOSITIVOS ////////////////////////////////////////////////////////////////////////////////////////
                String obtenerValoresSQL = "SELECT ID_DISPOSITIVO, REPUTACION FROM DISPOSITIVOS_INFO";
                Statement stmtObtener = conn.createStatement();
                ResultSet rs = stmtObtener.executeQuery(obtenerValoresSQL);

                Map<Integer, Integer> reputaciones = new HashMap<>();

                while (rs.next()) {
                    int dispositivoID = rs.getInt("ID_DISPOSITIVO");
                    int reputacion = rs.getInt("REPUTACION");

                    // Almacenar el ID del dispositivo y su reputación más reciente en un mapa
                    reputaciones.put(dispositivoID, reputacion);
                }

                ///////////////////////////////////////////////// INSERTAR INFORMACIÓN EN LA TABLA DE REPUTACIÓN ////////////////////////////////////////////////////////////////////////////////////////
                for (Map.Entry<Integer, Integer> entry : reputaciones.entrySet()) {
                    int dispositivoID = entry.getKey();
                    int reputacion = entry.getValue();

                    // Generar un timestamp actual en Java
                    long timestampMillis = System.currentTimeMillis();
                    Timestamp timestamp = new Timestamp(timestampMillis);

                    pstmtReputacion.setInt(1, dispositivoID);
                    pstmtReputacion.setString(2, timestamp.toString());
                    pstmtReputacion.setInt(3, reputacion);

                    pstmtReputacion.executeUpdate();
                }

                System.out.println("Datos de reputación actualizados insertados correctamente...");
            } else {
                System.out.println("REPUTACION table already exists in the database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            // Cerrar recursos
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        //System.out.println(ID_existente(args[0], operatorThings));

                        String caso = args[0];
                        System.out.println(ID_existente(caso, operatorThings));
                        //System.out.println(ID_existente("{\"thingID\":3,\"anomaliaID\":3,\"probabilidad\":1,\"gravedad\":1,\"detectabilidad\":1,\"amenaza\":\"Vulnerabilidad CVE-103\",\"descripcion\":\"Dispositivo nuevo que será incluido\"}", operatorThings));
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        
       
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}





