package c15.dev.gestioneMisurazione.controller;

import c15.dev.gestioneMisurazione.misurazioneAdapter.DispositivoMedicoAdapter;
import c15.dev.gestioneMisurazione.misurazioneAdapter.DispositivoMedicoStub;
import c15.dev.gestioneMisurazione.service.GestioneMisurazioneService;
import c15.dev.gestioneUtente.service.GestioneUtenteService;
import c15.dev.model.entity.Paziente;
import c15.dev.model.entity.MisurazionePressione;
import c15.dev.model.entity.Misurazione;
import c15.dev.model.entity.MisurazioneGlicemica;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Paolo Carmine Valletta, Alessandro Zoccola.
 * creato il: 04/01/2023.
 * Controller per le operazioni legate alle misurazioni.
 */
@RestController
@CrossOrigin
public class GestioneMisurazioneController {
    /**
     * consente di fare richieste via http.
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * Service per la misurazione.
     */
    @Autowired
    private GestioneMisurazioneService misurazioneService;

    /**
     * Service per la gestione utenti.
     */
    @Autowired
    private GestioneUtenteService utenteService;
    /**
     * stub dispositivo medico.
     */
    private DispositivoMedicoStub dispositivoMedicoStub
            = new DispositivoMedicoStub();

    /**
     * Metodo per la registrazione del dispositivo.
     * pre: il metodo deve essere chiamato solo da un paziente.
     * @param requestMap body della http request.
     * @param request http servlet.
     * @return response con relativo stato.
     */
    @PostMapping(value = "/dispositivo/registra")
    public ResponseEntity<Object>
    registraDispositivo(@RequestBody final HashMap<String, String> requestMap,
                        final HttpServletRequest request) {
        var email = request.getUserPrincipal().getName();
        var user = utenteService.findUtenteByEmail(email);
        var idUser = user.getId();

        if (!utenteService.isPaziente(user.getId())) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        var result = misurazioneService
                .registrazioneDispositivo(requestMap, idUser);
        if (!result) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Metodo per la rimozione del dispositivo.
     * pre: il metodo deve essere chiamato solo da un paziente.
     * @param map dispositivo da rimuovere.
     * @param request è la richiesta http.
     * @return response con relativo stato.
     */
    @RequestMapping(value = "/rimuoviDispositivo", method = RequestMethod.POST)
    public ResponseEntity<Object>
    rimozioneDispositivo(@RequestBody final HashMap<String, Object> map,
                         final HttpServletRequest request) {
        var email = request.getUserPrincipal().getName();
        var usr = utenteService.findUtenteByEmail(email);
        var idDispositivo = Long.parseLong(map.get("id").toString());

        var dispositivo = misurazioneService.getById(idDispositivo);
        misurazioneService.rimozioneDispositivo(dispositivo, usr.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *
     * @param id id paziente.
     * @return List di misurazione elenco delle misurazioni.
     */
    @PostMapping(value = "/FascicoloSanitarioElettronico")
    public List<Misurazione>
    getFascicoloSanitarioElettronico(@RequestParam final long id) {
        if (!utenteService.isPaziente(id)) {
            return null;
        }
        return misurazioneService.getMisurazioniByPaziente(id);
    }

    /**
     * pre solo un utente registrato come paziente può accedere a questa
     * funzionalitò.
     * @param request servlet.
     * @param map body della richiesta.
     * @return Misurazione singola misurazione.
     * Questo metodo permette di avviare una registrazione sull'id.
     * del dispositivo passato input e di restituire la misurazione generata.
     *
     */
    @PostMapping(value = "/avvioMisurazione")
    public Misurazione
    avvioMisurazione(@RequestBody final HashMap<String, Object> map,
                     final HttpServletRequest request) {

        var email = request.getUserPrincipal().getName();
        var u = utenteService.findUtenteByEmail(email);
        if (u == null || !utenteService.isPaziente(u.getId())) {
            return null;
        }

        Long idDispositivo = Long
                .parseLong(map.get("idDispositivo")
                .toString());
        var dispositivoMedico = misurazioneService.getById(idDispositivo);
        var dispositivoAdapter =
                new DispositivoMedicoAdapter(dispositivoMedico);
        var m =  dispositivoAdapter.avvioMisurazione();

        misurazioneService.save(m);
        return m;
    }

    /**
     * Metodo per ricevere le misurazioni tramite una categorie.
     * @param bo body della richiesta.
     * @return elenco misurazioni di una specifica categoria.
     */
    @PostMapping(value = "/getMisurazioneCategoria")
    public List<Misurazione>
    getMisurazioniByCategoria(@RequestBody final HashMap<String, Object> bo) {
        String cat = bo.get("categoria").toString();
        Long idPaz = Long.parseLong(bo.get("id").toString());
        return misurazioneService.getMisurazioneByCategoria(cat, idPaz);
    }

    /**
     * Metodo per ricevere le misurazioni da un paziente.
     * La differenza fra questo e il fascicolo medico sta nel fatto
     * che qui le misurazioni sono misurazioneDTO il che semplifica
     * alcuni task nel frontend.R
     * @param body body della richiesta.
     * @return elenco delle misurazioni del paziente passato in input.
     */
    @PostMapping(value = "/getAllMisurazioniByPaziente")
    public ResponseEntity<Object>
    getAllMisurazioniByPaziente(@RequestBody
                                 final HashMap<String, Object> body) {
        Long idPaz = Long.parseLong(body.get("id").toString());
        var list = misurazioneService.getAllMisurazioniByPaziente(idPaz);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * Metodo per ricevere le cateogire delle misurazioni di un paziente.
     * @param body body della richiesta.
     * @return lista di stringhe che indicano tutte le
     * categorie delle misurazioni.
     */
    @PostMapping(value = "/getCategorie")
    public List<String>
    getCategorieByPaziente(final @RequestBody HashMap<String, Object> body) {
        Long idPaz = Long.parseLong(body.get("id").toString());
        return misurazioneService.findCategorieByPaziente(idPaz);
    }

    /**
     * Metodo per prevedere se l'utente avrà un infarto.
     * pre: il metodo deve essere chiamato solo da un paziente.
     * @param body body della richiesta.
     * @param request servlet.
     * @return risultato della predizione 1 o 0.
     */
    @PostMapping(value = "/avvioPredizione")
    public ResponseEntity<Object>
    avvioPredizione(@RequestBody final HashMap<String, String> body,
                    final HttpServletRequest request) {

        var email = request.getUserPrincipal().getName();
        var usr = utenteService.findUtenteByEmail(email);
        Paziente paz = (Paziente) usr;
        long id = usr.getId();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        var eta = Period
                        .between(usr.getDataDiNascita(), LocalDate.now())
                        .getYears();

        map.put("age", eta);

        var sex = usr.getGenere();
        if (sex.equals("M")) {
            map.put("sex", 1);
        } else {
            map.put("sex", 0);
        }

        var pressione = paz.getMisurazione()
                .stream()
                .filter(s -> s.getClass().equals(MisurazionePressione.class))
                .map(s1 -> (MisurazionePressione) s1)
                .reduce((one, two) -> two)
                .get();

        map.put("trestbps", pressione.getPressioneMedia());

        var colesterolo = paz.getMisurazione()
                .stream()
                .filter(s -> s.getClass().equals(MisurazioneGlicemica.class))
                .map(s1 -> (MisurazioneGlicemica) s1)
                .reduce((one, two) -> two)
                .get();

        map.put("chol", colesterolo.getColesterolo());

        var fbs = paz.getMisurazione()
                .stream()
                .filter(s -> s.getClass().equals(MisurazioneGlicemica.class))
                .map(s -> (MisurazioneGlicemica) s)
                .reduce((one, two) -> two)
                .get();
        int flag = (fbs.getZuccheriNelSangue() > 120) ? 1 : 0;
        map.put("fbs", flag);
        map.put("thalach", pressione.getBattitiPerMinuto());

        if (body.get("infarto").equals("si")) {
            map.put("thal", 1);
        } else {
            map.put("thal", 0);
        }

        var i =  restTemplate.postForObject("http://localhost:8083/",
                                                        map,
                                                        String.class);
        i = i.substring(1, 2);
        return new ResponseEntity<>(Integer.valueOf(i), HttpStatus.OK);
    }


}
