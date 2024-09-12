package io.broker.Dispositivos;

import java.sql.Date;
import java.time.LocalDate;

public class DispVirtual extends Dispositivo{

    public DispVirtual(int id, int numVulnerabilidades, boolean tieneCertificado, LocalDate ultimaRevision,
            int tiempoFuncionamiento, int actualizacionesFirmware, int frecuenciaComunicacion, int historialFallos,
            int reputacionInicial, String tipo, String estado) {
        super(id, numVulnerabilidades, tieneCertificado, ultimaRevision, tiempoFuncionamiento, actualizacionesFirmware,
                frecuenciaComunicacion, historialFallos, reputacionInicial, tipo, estado);
        //TODO Auto-generated constructor stub
    }

    
    
}
