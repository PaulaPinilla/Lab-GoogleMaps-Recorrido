package co.edu.unipiloto.rutasmaps;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ================= PARTE A: DATOS DEL RECOLECTOR =================

    // Punto de partida del recolector: Universidad Piloto (Bogotá)
    private static final String ORIGEN_NOMBRE = "Universidad Piloto (bodega)";
    private static final double ORIGEN_LAT = 4.632339710;
    private static final double ORIGEN_LNG = -74.065350;

    // Encomiendas asignadas al recolector (nombre, latitud, longitud)
    private final String[] encomiendaNombre = {
            "Centro Comercial Andino",
            "Parque Simón Bolívar",
            "Museo del Oro",
            "Centro Comercial Unicentro"
    };
    private final double[] encomiendaLat = {4.6667, 4.6583, 4.6019, 4.7024};
    private final double[] encomiendaLng = {-74.0527, -74.0937, -74.0721, -74.0415};

    // ================= PARTE B: DATOS DE TUTORÍAS =================

    // Ubicación del estudiante que busca tutoría
    private static final String ESTUDIANTE_NOMBRE = "Estudiante (Galerías)";
    private static final double ESTUDIANTE_LAT = 4.6430;
    private static final double ESTUDIANTE_LNG = -74.0770;

    // Docentes disponibles (nombre, latitud, longitud)
    private final String[] docenteNombre = {
            "Docente de Matemáticas - Usaquén",
            "Docente de Programación - Teusaquillo",
            "Docente de Física - Suba"
    };
    private final double[] docenteLat = {4.6950, 4.6370, 4.7400};
    private final double[] docenteLng = {-74.0310, -74.0800, -74.0830};

    // Aquí guardamos cuál docente quedó más cerca (-1 = todavía no se ha buscado)
    private int docenteCercano = -1;

    // Textos de la pantalla
    private TextView tvEncomiendas;
    private TextView tvOrdenRuta;
    private TextView tvDocentes;
    private TextView tvDocenteCercano;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEncomiendas = findViewById(R.id.tvEncomiendas);
        tvOrdenRuta = findViewById(R.id.tvOrdenRuta);
        tvDocentes = findViewById(R.id.tvDocentes);
        tvDocenteCercano = findViewById(R.id.tvDocenteCercano);

        // Mostrar la lista de encomiendas
        String textoEncomiendas = "Salida: " + ORIGEN_NOMBRE + "\n";
        for (int i = 0; i < encomiendaNombre.length; i++) {
            textoEncomiendas = textoEncomiendas + (i + 1) + ". " + encomiendaNombre[i] + "\n";
        }
        tvEncomiendas.setText(textoEncomiendas);

        // Mostrar la lista de docentes
        String textoDocentes = "Ubicación: " + ESTUDIANTE_NOMBRE + "\nDocentes:\n";
        for (int i = 0; i < docenteNombre.length; i++) {
            textoDocentes = textoDocentes + "- " + docenteNombre[i] + "\n";
        }
        tvDocentes.setText(textoDocentes);
    }

    // ================= REQUISITO 2: SEÑALAR LA UBICACIÓN =================

    // Muestra en Google Maps la ubicación de una encomienda (el número viene del tag del botón)
    public void onVerEncomienda(View view) {
        int i = Integer.parseInt(view.getTag().toString());
        String etiqueta = Uri.encode(encomiendaNombre[i]);
        Uri gmmIntentUri = Uri.parse("geo:" + encomiendaLat[i] + "," + encomiendaLng[i]
                + "?q=" + encomiendaLat[i] + "," + encomiendaLng[i] + "(" + etiqueta + ")");
        abrirGoogleMaps(gmmIntentUri);
    }

    // ================= REQUISITO 1: RUTA DEL DÍA MÁS EFICIENTE =================

    // Ordena las encomiendas con el método del "vecino más cercano"
    // y abre Google Maps con todas las paradas en ese orden
    public void onPlanearRuta(View view) {
        int n = encomiendaNombre.length;
        boolean[] visitada = new boolean[n];
        int[] orden = new int[n];

        double latActual = ORIGEN_LAT;
        double lngActual = ORIGEN_LNG;
        double distanciaTotal = 0;

        for (int paso = 0; paso < n; paso++) {
            int mejor = -1;
            double menorDistancia = Double.MAX_VALUE;

            // Buscar la encomienda no visitada más cercana a donde estoy
            for (int j = 0; j < n; j++) {
                if (!visitada[j]) {
                    double d = distanciaKm(latActual, lngActual, encomiendaLat[j], encomiendaLng[j]);
                    if (d < menorDistancia) {
                        menorDistancia = d;
                        mejor = j;
                    }
                }
            }

            orden[paso] = mejor;
            visitada[mejor] = true;
            distanciaTotal = distanciaTotal + menorDistancia;
            latActual = encomiendaLat[mejor];
            lngActual = encomiendaLng[mejor];
        }

        // Mostrar el orden en pantalla
        String textoOrden = "Orden sugerido:\n";
        for (int paso = 0; paso < n; paso++) {
            textoOrden = textoOrden + (paso + 1) + ". " + encomiendaNombre[orden[paso]] + "\n";
        }
        textoOrden = textoOrden + String.format(Locale.US,
                "Distancia aproximada (línea recta): %.2f km", distanciaTotal);
        tvOrdenRuta.setText(textoOrden);

        // Construir la URL: la última encomienda es el destino, las demás son paradas (waypoints)
        int ultima = orden[n - 1];
        String waypoints = "";
        for (int paso = 0; paso < n - 1; paso++) {
            int k = orden[paso];
            if (paso > 0) {
                waypoints = waypoints + "|";
            }
            waypoints = waypoints + encomiendaLat[k] + "," + encomiendaLng[k];
        }

        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1"
                + "&origin=" + ORIGEN_LAT + "," + ORIGEN_LNG
                + "&destination=" + encomiendaLat[ultima] + "," + encomiendaLng[ultima]
                + "&waypoints=" + waypoints
                + "&travelmode=driving");
        abrirGoogleMaps(gmmIntentUri);
    }

    // ================= REQUISITO 3: DOCENTE MÁS CERCANO =================

    // Calcula cuál docente está más cerca del estudiante
    public void onBuscarDocente(View view) {
        double menorDistancia = Double.MAX_VALUE;
        for (int i = 0; i < docenteNombre.length; i++) {
            double d = distanciaKm(ESTUDIANTE_LAT, ESTUDIANTE_LNG, docenteLat[i], docenteLng[i]);
            if (d < menorDistancia) {
                menorDistancia = d;
                docenteCercano = i;
            }
        }
        tvDocenteCercano.setText(String.format(Locale.US,
                "Docente más cercano: %s (%.2f km)", docenteNombre[docenteCercano], menorDistancia));
    }

    // Abre la ruta hacia el docente más cercano en el modo elegido
    // (el modo viene del tag del botón: driving, walking o bicycling)
    public void onRutaDocente(View view) {
        if (docenteCercano == -1) {
            Toast.makeText(this, "Primero presiona 'Buscar docente más cercano'",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String modo = view.getTag().toString();
        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1"
                + "&origin=" + ESTUDIANTE_LAT + "," + ESTUDIANTE_LNG
                + "&destination=" + docenteLat[docenteCercano] + "," + docenteLng[docenteCercano]
                + "&travelmode=" + modo);
        abrirGoogleMaps(gmmIntentUri);
    }

    // ================= MÉTODOS DE APOYO =================

    // Invoca el intent de Google Maps (igual que en la diapositiva 16 de la guía)
    private void abrirGoogleMaps(Uri gmmIntentUri) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                // Si no está Google Maps, se abre con cualquier app de mapas o el navegador
                Intent intentSinPaquete = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                startActivity(intentSinPaquete);
            } catch (ActivityNotFoundException innerEx) {
                Toast.makeText(this, "Por favor instala una aplicación de mapas",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Distancia en kilómetros entre dos coordenadas (fórmula de Haversine)
    private double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double radioTierra = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radioTierra * c;
    }
}