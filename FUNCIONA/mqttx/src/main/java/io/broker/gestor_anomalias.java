package io.broker;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import com.google.gson.Gson;
import io.broker.Anomalias.Anomalia;

public class gestor_anomalias {
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // RECUPERAR IDs VÁLIDOS
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MAIN
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // CREACIÓN DE ANOMALÍAS ALEATORIAS
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // int[] VALORES_PERMITIDOS = {1, 3, 9};
        // SecureRandom random = new SecureRandom();
        
        // // Número de anomalías a generar
        // int numAnomalias = 5;

        // Lista donde se guardan las anomalías para ser enviadas
        List<Anomalia> anomaList = new LinkedList<Anomalia>();

        // // Generar y mostrar las anomalías aleatorias
        // System.out.println("Anomalías generadas:");
        // for (int i = 0; i < numAnomalias; i++) {
        //     // Seleccionar un valor aleatorio de los valores permitidos para probabilidad, detectabilidad y gravedad
        //     int probabilidad = VALORES_PERMITIDOS[random.nextInt(VALORES_PERMITIDOS.length)];
        //     int detectabilidad = VALORES_PERMITIDOS[random.nextInt(VALORES_PERMITIDOS.length)];
        //     int gravedad = VALORES_PERMITIDOS[random.nextInt(VALORES_PERMITIDOS.length)];
            
        //     // Otros valores aleatorios
        //     int thingID = random.nextInt(1000); // Ejemplo: ID del dispositivo entre 0 y 999
        //     int anomaliaID = random.nextInt(100); // Ejemplo: ID de la anomalía entre 0 y 99
        //     String amenaza = "Vulnerabilidad CVE-" + (i + 1); // Ejemplo: Amenaza aleatoria
        //     String descripcion = "Esta vuln consiste en ... " + (i + 1); // Ejemplo: Descripción aleatoria
            
        //     // Crear y mostrar la anomalía generada
        //     Anomalia anomalia = new Anomalia(thingID, anomaliaID, probabilidad, gravedad, detectabilidad, amenaza, descripcion);
        //     System.out.println(anomalia);

        //     anomaList.add(anomalia);
        // }

        // Anomalia anomaliaCoincide = new Anomalia(1, 1, 1, 1, 1, "CVE-0", "Descripción 0");
        // anomaList.add(anomaliaCoincide);
        // System.out.println(anomaliaCoincide);

        // // Caso 1: Dispositivo no existe (error)
        // // Este caso será manejado en tiempo de ejecución, no se necesita generar una anomalía específica
        // Anomalia anomaliaNoExiste = new Anomalia(0, 0, 1, 1, 1, "CVE-0", "Descripción 0");
        // anomaList.add(anomaliaNoExiste);

        // // Caso 2: Dispositivo nuevo, historial de reputación nulo, quedará prohibido
        // Anomalia anomaliaProhibido = new Anomalia(1, 1, 9, 9, 9, "Vulnerabilidad CVE-101", "Dispositivo nuevo que será prohibido");
        // anomaList.add(anomaliaProhibido);

        // Caso 3: Dispositivo nuevo, historial de reputación nulo, quedará en cuarentena
        Anomalia anomaliaCuarentena = new Anomalia(2, 2, 3, 3, 3, "Vulnerabilidad CVE-102", "Dispositivo nuevo que estará en cuarentena");
        anomaList.add(anomaliaCuarentena);

        // // Caso 4: Dispositivo nuevo, historial de reputación nulo, quedará incluido en la red
        // Anomalia anomaliaIncluido = new Anomalia(3, 3, 1, 1, 1, "Vulnerabilidad CVE-103", "Dispositivo nuevo que será incluido");
        // anomaList.add(anomaliaIncluido);

        // // Caso 5: Dispositivo con historial, quedará prohibido
        // Anomalia anomaliaHistorialProhibido = new Anomalia(4, 4, 9, 9, 9, "Vulnerabilidad CVE-104", "Dispositivo con historial que será prohibido");
        // anomaList.add(anomaliaHistorialProhibido);

        // // Caso 6: Dispositivo con historial, quedará en cuarentena
        // Anomalia anomaliaHistorialCuarentena = new Anomalia(5, 5, 3, 3, 3, "Vulnerabilidad CVE-105", "Dispositivo con historial que estará en cuarentena");
        // anomaList.add(anomaliaHistorialCuarentena);

        // // Caso 7: Dispositivo con historial, quedará incluido en la red
        // Anomalia anomaliaHistorialIncluido = new Anomalia(6, 6, 1, 1, 1, "Vulnerabilidad CVE-106", "Dispositivo con historial que será incluido");
        // anomaList.add(anomaliaHistorialIncluido);

        // // Caso 8: Dispositivo con nivel de riesgo alto, quedará prohibido
        // Anomalia casoEstudio8 = new Anomalia(7, 7, 10, "Vulnerabilidad CVE-107", "Nivel de riesgo muy alto, dispositivo prohibido");
        // anomaList.add(casoEstudio8);

        // // Caso 9: Dispositivo con nivel de riesgo medio, quedará en cuarentena
        // Anomalia casoEstudio9 = new Anomalia(8, 8, 5, "Vulnerabilidad CVE-108", "Nivel de riesgo medio, dispositivo en cuarentena");
        // anomaList.add(casoEstudio9);

        // // Caso 10: Dispositivo con nivel de riesgo bajo, quedará incluido en la red
        // Anomalia casoEstudio10 = new Anomalia(9, 9, 1, "Vulnerabilidad CVE-109", "Nivel de riesgo bajo, dispositivo permitido");
        // anomaList.add(casoEstudio10);


        // // TRAZA: Mostrar las anomalías generadas
        // System.out.println("Anomalías generadas:");
        // for (Anomalia anomalia : anomaList) {
        //     System.out.println(anomalia);
        // }

        // Convertir las anomalías a JSON
        Gson gson = new Gson();
        List<String> jsonAnomalies = new LinkedList<>();
        for (Anomalia anomalia : anomaList) {
            String json = gson.toJson(anomalia);
            jsonAnomalies.add(json);
        }

        // Imprimir las anomalías en formato JSON
        System.out.println("Anomalías en formato JSON:");
        for (String json : jsonAnomalies) {
            System.out.println(json);
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        
    //     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //     // SOCKETS
    //     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //     final String host = "localhost";
    //     final int puerto = 12345;
        
    //     try (Socket cliente = new Socket(host, puerto);
    //          BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
    //          PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
    //          BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in))) {

    //         System.out.println("Conectado al servidor en " + host + ":" + puerto);

    //         // Enviar mensaje al servidor
    //         for (Anomalia a : anomaList) {
                
    //             System.out.println("Envío de anomalía al servidor");
    //             String mensajeUsuario = a.toString();
    //             salida.println(mensajeUsuario);
                
    //             // Recibir respuesta del servidor
    //             String respuestaServidor = entrada.readLine();
    //             System.out.println("Respuesta del servidor: " + respuestaServidor);
    //             // Recibir respuesta del servidor
    //             String respuestaServidor2 = entrada.readLine();
    //             System.out.println("Respuesta del servidor: " + respuestaServidor2 + "\n");
    //         }

    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}


