package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.BestTime;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.RaceDayRecap;
import org.example.belgianslotclubspring.models.RaceTrackScratch;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.example.belgianslotclubspring.utils.RecapPhrases;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RaceDayRecapService {

    public RaceDayRecap build(List<Qualif> qualifications, List<RaceResult> raceResults) {
        List<Qualif> quals = qualifications == null ? List.of() : qualifications;
        List<RaceResult> results = raceResults == null ? List.of() : raceResults;

        List<String> qualiHeadlines = buildQualiHeadlines(quals);
        List<String> raceHeadlines = new ArrayList<>();
        List<String> podium = new ArrayList<>();
        List<RaceTrackScratch> trackScratches = new ArrayList<>();
        List<String> scratchLeaders = new ArrayList<>();

        if (!results.isEmpty()) {
            buildRaceNarrative(results, raceHeadlines, podium, trackScratches, scratchLeaders);
        }

        return new RaceDayRecap(
                !quals.isEmpty(),
                !results.isEmpty(),
                qualiHeadlines,
                raceHeadlines,
                podium,
                trackScratches,
                scratchLeaders
        );
    }

    private List<String> buildQualiHeadlines(List<Qualif> quals) {
        List<String> lines = new ArrayList<>();
        if (quals.isEmpty()) {
            return lines;
        }

        String poleName = quals.get(0).getPilotName();
        int n = quals.size();
        int base = RecapPhrases.seed("quali", poleName, n);

        String pluralPilots = n > 1
                ? n + " pilotes se sont élancés"
                : "un pilote s’est élancé";
        lines.add(RecapPhrases.pick(base,
                "Les chronos de qualifs tombent : " + pluralPilots + " pour la pole.",
                "Session qualificative bouclée — " + pluralPilots + " pour décrocher la première ligne.",
                "La grille se dessine : " + pluralPilots + " dans la bataille pour la pole.",
                "Fin des essais chrono : " + pluralPilots + " ont cherché le tour magique.",
                "Qualifs terminées. " + (n > 1 ? n + " chronos au compteur" : "Un chrono au compteur")
                        + ", et une pole à attribuer."
        ));

        Qualif pole = quals.get(0);
        String poleTime = formatTime(pole.getBestTime());
        lines.add(RecapPhrases.pick(base + 1,
                "La pole revient à " + poleName + " en " + poleTime
                        + ". Un chrono qui plante le décor de la journée.",
                poleName + " s’installe en tête de grille (" + poleTime
                        + "). Voilà une référence qui va peser.",
                "Premier sur la feuille : " + poleName + ", auteur d’un " + poleTime
                        + " qui fait taire le paddock.",
                poleName + " arrache la pole en " + poleTime
                        + " — une entrée en matière musclée.",
                "C’est " + poleName + " qui verrouille la pole position (" + poleTime
                        + "). Le ton est donné."
        ));

        if (quals.size() >= 2) {
            Qualif second = quals.get(1);
            double gap = second.getBestTime() - pole.getBestTime();
            String gapFmt = formatGap(gap);
            String secondName = second.getPilotName();
            if (gap < 0.15) {
                lines.add(RecapPhrases.pick(base + 2,
                        "Suspense dès les qualifs ! " + secondName + " n’est qu’à " + gapFmt
                                + " — la grille va être brûlante.",
                        "Presque rien entre " + poleName + " et " + secondName + " (" + gapFmt
                                + "). La première ligne est un vrai duel.",
                        secondName + " colle à " + gapFmt + " de la pole. Autant dire que la course"
                                + " partira sur un fil.",
                        "Écart microscopique : " + secondName + " à " + gapFmt
                                + ". Les deux premiers sont dans le même souffle."
                ));
            } else if (gap < 0.5) {
                lines.add(RecapPhrases.pick(base + 2,
                        secondName + " reste dans le match, à " + gapFmt + " de la pole.",
                        "Deuxième chrono pour " + secondName + " (" + gapFmt
                                + ") : assez près pour croire à une remontada.",
                        secondName + " pointe à " + gapFmt
                                + " — un écart jouable dès le premier virage.",
                        "À " + gapFmt + ", " + secondName
                                + " n’a pas dit son dernier mot avant le départ."
                ));
            } else {
                lines.add(RecapPhrases.pick(base + 2,
                        poleName + " a créé un vrai écart : " + secondName
                                + " pointe à " + gapFmt + ".",
                        "La pole de " + poleName + " fait mal : " + secondName
                                + " est déjà à " + gapFmt + ".",
                        secondName + " doit digérer " + gapFmt
                                + " de retard dès les qualifs — mission délicate.",
                        "Écart net dès la grille : " + gapFmt + " séparent "
                                + poleName + " de " + secondName + "."
                ));
            }
        }

        if (quals.size() >= 3) {
            Qualif third = quals.get(2);
            String thirdName = third.getPilotName();
            String thirdTime = formatTime(third.getBestTime());
            lines.add(RecapPhrases.pick(base + 3,
                    "Troisième chrono : " + thirdName + " (" + thirdTime + ").",
                    thirdName + " s’empare de la troisième place sur la grille en " + thirdTime + ".",
                    "Derrière le duo de tête, " + thirdName + " signe un " + thirdTime + ".",
                    "P3 pour " + thirdName + " (" + thirdTime
                            + ") — une belle base pour la course."
            ));
        }

        if (quals.size() >= 4) {
            Qualif last = quals.get(quals.size() - 1);
            double gapLast = last.getBestTime() - pole.getBestTime();
            String lastName = last.getPilotName();
            String gapLastFmt = formatGap(gapLast);
            lines.add(RecapPhrases.pick(base + 4,
                    lastName + " ferme la grille à " + gapLastFmt
                            + " de la pole — tout reste à jouer en course.",
                    "En fond de grille, " + lastName + " (" + gapLastFmt
                            + ") saura qu’il faudra attaquer dès le départ.",
                    lastName + " part de loin (" + gapLastFmt
                            + ") : les courses se gagnent aussi dans le trafic.",
                    "Dernier chrono pour " + lastName + " à " + gapLastFmt
                            + " — l’histoire de la course n’est pas encore écrite."
            ));
        }

        return lines;
    }

    private void buildRaceNarrative(
            List<RaceResult> results,
            List<String> headlines,
            List<String> podium,
            List<RaceTrackScratch> trackScratches,
            List<String> scratchLeaders
    ) {
        RaceResult winner = results.get(0);
        String winnerName = winner.getNom();
        int n = results.size();
        int base = RecapPhrases.seed("race", winnerName, n, formatLaps(winner.getTotalTours()));

        headlines.add(RecapPhrases.pick(base,
                "Drapeau à damier : " + n + " pilote" + (n > 1 ? "s" : "")
                        + " ont croisé la ligne.",
                "La course est terminée — " + n + " chronos définitifs au tableau.",
                "Fin de séance de course : " + n + " concurrent"
                        + (n > 1 ? "s" : "") + " au classement.",
                "Voilà, c’est plié. " + n + " pilote" + (n > 1 ? "s" : "")
                        + " au chronométrage final.",
                "Le chronomètre s’arrête : " + n + " résultat"
                        + (n > 1 ? "s" : "") + " à analyser."
        ));

        String tours = formatLaps(winner.getTotalTours());
        headlines.add(RecapPhrases.pick(base + 1,
                "Victoire pour " + winnerName + " avec " + tours
                        + " tours ! Une journée réussie du début à la fin.",
                winnerName + " s’impose (" + tours
                        + " tours) — une performance pleine d’autorité.",
                "C’est " + winnerName + " qui l’emporte au bout de " + tours
                        + " tours. Mission accomplie.",
                winnerName + " raflé la mise : " + tours
                        + " tours au compteur et les honneurs de la journée.",
                "Premier au classement : " + winnerName + " (" + tours
                        + " tours). Une victoire qui se mérite."
        ));

        if (results.size() >= 2) {
            RaceResult second = results.get(1);
            double gapLaps = winner.getTotalTours() - second.getTotalTours();
            String gapFmt = formatLaps(gapLaps);
            String secondName = second.getNom();
            String tourWord = Math.abs(gapLaps - 1.0) < 0.001 ? "tour" : "tours";
            if (gapLaps < 1.0) {
                headlines.add(RecapPhrases.pick(base + 2,
                        "Quel thriller ! " + secondName + " termine à seulement "
                                + gapFmt + " " + tourWord + " — on a frôlé le coup de théâtre.",
                        "Presque ! " + secondName + " n’a manqué la victoire que de "
                                + gapFmt + " " + tourWord + ". Un souffle.",
                        secondName + " finit à " + gapFmt + " " + tourWord
                                + " : du grand spectacle jusqu’au bout.",
                        "Écart de " + gapFmt + " " + tourWord + " pour " + secondName
                                + " — la décision s’est jouée sur un détail."
                ));
            } else if (gapLaps < 3.0) {
                headlines.add(RecapPhrases.pick(base + 2,
                        secondName + " reste très proche, à " + gapFmt
                                + " tours. Belle bagarre.",
                        "Deuxième pour " + secondName + " (" + gapFmt
                                + " tours) : une course accrochée du début à la fin.",
                        secondName + " ne lâche rien et termine à " + gapFmt
                                + " tours du vainqueur.",
                        "À " + gapFmt + " tours, " + secondName
                                + " prouve que le podium se mérite aussi dans l’effort."
                ));
            } else {
                headlines.add(RecapPhrases.pick(base + 2,
                        secondName + " prend la deuxième place ("
                                + formatLaps(second.getTotalTours()) + " tours), à "
                                + gapFmt + " tours du vainqueur.",
                        "P2 pour " + secondName + " : solide, mais à " + gapFmt
                                + " tours de " + winnerName + ".",
                        secondName + " monte sur la deuxième marche avec "
                                + formatLaps(second.getTotalTours())
                                + " tours — " + gapFmt + " de retard sur la tête.",
                        winnerName + " a su creuser : " + secondName + " est à "
                                + gapFmt + " tours."
                ));
            }
        }

        if (results.size() >= 3) {
            RaceResult third = results.get(2);
            String thirdName = third.getNom();
            String thirdTours = formatLaps(third.getTotalTours());
            headlines.add(RecapPhrases.pick(base + 3,
                    "Et " + thirdName + " complète le podium avec " + thirdTours + " tours.",
                    "Troisième marche pour " + thirdName + " (" + thirdTours + " tours).",
                    thirdName + " glisse sur le podium : " + thirdTours
                            + " tours et la récompense d’une belle course.",
                    "P3 acquis pour " + thirdName + " — " + thirdTours
                            + " tours au total."
            ));
        }

        for (int i = 0; i < Math.min(3, results.size()); i++) {
            RaceResult r = results.get(i);
            podium.add((i + 1) + ". " + r.getNom() + " — " + formatLaps(r.getTotalTours()) + " tours");
        }

        Map<String, Integer> scratchCount = new HashMap<>();
        Map<String, List<Integer>> scratchTracks = new HashMap<>();

        for (int track = 1; track <= 6; track++) {
            String bestPilot = null;
            double bestLap = Double.MAX_VALUE;
            String worstPilot = null;
            double worstLap = -1;

            for (RaceResult r : results) {
                Double lap = bestLapOnTrack(r, track);
                if (lap == null || lap <= 0) {
                    continue;
                }
                if (lap < bestLap) {
                    bestLap = lap;
                    bestPilot = r.getNom();
                }
                if (lap > worstLap) {
                    worstLap = lap;
                    worstPilot = r.getNom();
                }
            }

            if (bestPilot == null) {
                continue;
            }

            scratchCount.merge(bestPilot, 1, Integer::sum);
            scratchTracks.computeIfAbsent(bestPilot, k -> new ArrayList<>()).add(track);

            String gapFmt = "—";
            if (worstPilot != null && !worstPilot.equals(bestPilot) && worstLap > bestLap) {
                gapFmt = formatGap(worstLap - bestLap);
            }

            trackScratches.add(new RaceTrackScratch(
                    track,
                    bestPilot,
                    formatTime(bestLap),
                    worstPilot != null ? worstPilot : "—",
                    gapFmt
            ));
        }

        List<Map.Entry<String, Integer>> leaders = scratchCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();

        if (!leaders.isEmpty()) {
            Map.Entry<String, Integer> top = leaders.get(0);
            List<Integer> tracks = scratchTracks.getOrDefault(top.getKey(), List.of());
            String tracksLabel = tracks.stream().map(nTrack -> "piste " + nTrack)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            String topName = top.getKey();
            int count = top.getValue();
            int scratchSeed = RecapPhrases.seed("scratch", topName, count, tracksLabel);

            if (count >= 3) {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        "Quel festival de scratchs pour " + topName + " ! " + count
                                + " pistes dominées (" + tracksLabel
                                + "). Le genre de journée dont on se souvient.",
                        topName + " a maraudé " + count + " scratchs (" + tracksLabel
                                + "). Une démonstration de vitesse pure.",
                        "Vitesse pure : " + topName + " signe " + count
                                + " meilleurs tours (" + tracksLabel + "). Impressionnant.",
                        count + " pistes au nom de " + topName + " (" + tracksLabel
                                + ") — difficile de faire plus clair."
                ));
            } else if (count == 2) {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        topName + " a mis deux fois tout le monde d’accord ("
                                + tracksLabel + "). Une vraie pointe de vitesse.",
                        "Double scratch pour " + topName + " (" + tracksLabel
                                + ") : deux claques chronométrées.",
                        topName + " s’offre un duo gagnant sur " + tracksLabel
                                + ". La pointe était là.",
                        "Deux pistes, deux scratchs : " + topName
                                + " a trouvé le rythme (" + tracksLabel + ")."
                ));
            } else {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        "Le scratch du jour revient à " + topName + " sur la "
                                + tracksLabel + ".",
                        "Meilleur tour de référence : " + topName + " sur la "
                                + tracksLabel + ".",
                        topName + " s’adjuge le scratch de la " + tracksLabel + ".",
                        "Sur la " + tracksLabel + ", personne n’a fait mieux que "
                                + topName + "."
                ));
            }
            scratchLeaders.add(topName + " — " + count
                    + " piste" + (count > 1 ? "s" : "")
                    + (tracksLabel.isEmpty() ? "" : " (" + tracksLabel + ")"));

            if (leaders.size() > 1) {
                Map.Entry<String, Integer> second = leaders.get(1);
                String secondName = second.getKey();
                int secondCount = second.getValue();
                headlines.add(RecapPhrases.pick(scratchSeed + 1,
                        "Pas loin derrière au rayon scratchs : " + secondName
                                + " avec " + secondCount + " piste"
                                + (secondCount > 1 ? "s" : "") + ".",
                        secondName + " répond aussi présent" + (secondCount > 1 ? "" : "")
                                + " : " + secondCount + " scratch"
                                + (secondCount > 1 ? "s" : "") + " au compteur.",
                        "Autre homme fort des meilleurs tours : " + secondName
                                + " (" + secondCount + ").",
                        "Dans le sillage, " + secondName + " glane " + secondCount
                                + " piste" + (secondCount > 1 ? "s" : "") + "."
                ));
                List<Integer> t2 = scratchTracks.getOrDefault(secondName, List.of());
                scratchLeaders.add(secondName + " — " + secondCount
                        + " piste" + (secondCount > 1 ? "s" : "")
                        + (t2.isEmpty() ? "" : " (" + t2.stream().map(tn -> "piste " + tn)
                        .reduce((a, b) -> a + ", " + b).orElse("") + ")"));
            }
        }

        RaceTrackScratch extreme = null;
        double extremeGap = -1;
        for (RaceTrackScratch st : trackScratches) {
            if (st.worstGapFormatted() == null || "—".equals(st.worstGapFormatted())) {
                continue;
            }
            try {
                String g = st.worstGapFormatted().startsWith("+")
                        ? st.worstGapFormatted().substring(1)
                        : st.worstGapFormatted();
                Double gap = RallyeTimeFormat.parse(g);
                if (gap != null && gap > extremeGap) {
                    extremeGap = gap;
                    extreme = st;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (extreme != null && extremeGap > 0 && extremeGap < 0.3 && results.size() >= 3) {
            int s = RecapPhrases.seed("tight", extreme.trackNumber(), extreme.pilotName());
            headlines.add(RecapPhrases.pick(s,
                    "Sur la piste " + extreme.trackNumber()
                            + ", le plateau était ultra-serré : scratch pour "
                            + extreme.pilotName() + " en " + extreme.bestLapFormatted()
                            + ", et seulement " + extreme.worstGapFormatted()
                            + " jusqu’au plus lent. Du grand sport.",
                    "Piste " + extreme.trackNumber() + " : tout le monde dans un mouchoir"
                            + " (" + extreme.worstGapFormatted() + " d’écart max). Scratch pour "
                            + extreme.pilotName() + ".",
                    "La piste " + extreme.trackNumber() + " a livré un vrai festival de précision"
                            + " — " + extreme.pilotName() + " devant, mais seulement "
                            + extreme.worstGapFormatted() + " jusqu’au fond de grille.",
                    "Écarts microscopiques sur la piste " + extreme.trackNumber()
                            + " : " + extreme.pilotName() + " en " + extreme.bestLapFormatted()
                            + ", peloton collé derrière."
            ));
        } else if (extreme != null && extremeGap >= 1.0) {
            int s = RecapPhrases.seed("blow", extreme.trackNumber(), extreme.pilotName());
            headlines.add(RecapPhrases.pick(s,
                    "La piste " + extreme.trackNumber() + " a fait des dégâts : "
                            + extreme.pilotName() + " a envolé le chrono ("
                            + extreme.bestLapFormatted()
                            + "), pendant que d’autres perdaient gros.",
                    "Explosion de rythme sur la piste " + extreme.trackNumber()
                            + " : " + extreme.pilotName() + " signe un "
                            + extreme.bestLapFormatted() + " qui fait mal.",
                    "Sur la piste " + extreme.trackNumber() + ", " + extreme.pilotName()
                            + " a littéralement écrasé le chrono ("
                            + extreme.bestLapFormatted() + ").",
                    "La piste " + extreme.trackNumber() + " a séparé le plateau : "
                            + extreme.pilotName() + " au sommet ("
                            + extreme.bestLapFormatted() + "), écarts énormes derrière."
            ));
        }

        if (results.size() >= 3) {
            RaceResult last = results.get(results.size() - 1);
            double gapLast = winner.getTotalTours() - last.getTotalTours();
            String lastName = last.getNom();
            String gapLastFmt = formatLaps(gapLast);
            int s = RecapPhrases.seed("last", lastName, gapLastFmt);
            headlines.add(RecapPhrases.pick(s,
                    lastName + " termine avec " + gapLastFmt
                            + " tours de retard : la journée n’a pas souri,"
                            + " mais le championnat est long.",
                    "Plus loin, " + lastName + " finit à " + gapLastFmt
                            + " tours — une édition à digérer, déjà l’envie de se rattraper.",
                    lastName + " ferme la marche (" + gapLastFmt
                            + " tours) : pas la journée rêvée, loin d’être la dernière.",
                    "Dernier au classement, " + lastName + " concède " + gapLastFmt
                            + " tours. La suite du calendrier offrira sa revanche."
            ));
        }

        headlines.add(RecapPhrases.pick(base + 9,
                "Bilan : " + winnerName + " repart avec les honneurs. Bravo à tout le plateau !",
                "Pour résumer : journée de " + winnerName
                        + ". Merci à tous les pilotes pour le spectacle.",
                "On retiendra surtout la victoire de " + winnerName
                        + " — et une belle bataille collective.",
                "Fin de chapitre pour cette course : " + winnerName
                        + " au sommet, rendez-vous à la prochaine.",
                winnerName + " a écrit le dénouement. Clap de fin — et déjà l’appétit pour la suite."
        ));
    }

    private static Double bestLapOnTrack(RaceResult result, int trackNumber) {
        if (result.getBestTime() == null) {
            return null;
        }
        for (BestTime bt : result.getBestTime()) {
            if (bt.getTrackNumber() == trackNumber) {
                return bt.getBestLapTime();
            }
        }
        int idx = trackNumber - 1;
        if (idx >= 0 && idx < result.getBestTime().size()) {
            return result.getBestTime().get(idx).getBestLapTime();
        }
        return null;
    }

    private static String formatTime(double seconds) {
        return RallyeTimeFormat.format(seconds);
    }

    private static String formatGap(double gapSeconds) {
        return RallyeTimeFormat.formatGap(gapSeconds);
    }

    private static String formatLaps(double laps) {
        if (Math.abs(laps - Math.rint(laps)) < 0.001) {
            return String.valueOf((long) Math.rint(laps));
        }
        return String.format(Locale.US, "%.1f", laps);
    }
}
