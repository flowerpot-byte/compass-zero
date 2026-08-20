# -*- coding: utf-8 -*-
"""Die sechs Datensorten der Ueberlebenskarte -- an genau einer Stelle.

Alles andere aus OpenStreetMap faellt weg. Diese Auswahl ist keine
Geschmacksfrage, sondern der Kern der Groessenrechnung: Die Rohdaten von
Oesterreich sind 806 MB, und der weit groesste Teil davon sind Haeuser,
Hausnummern, Ladenoeffnungszeiten, Parkbaenke und Briefkaesten -- Dinge, die
auf einer Karte fuer den Notfall nichts verloren haben.

WARUM DIE LISTE HIER STEHT UND NICHT IM MESS-SKRIPT: Gemessen wird mehrmals,
mit verschiedenen Zoomtiefen und Vereinfachungen. Wenn die Auswahl dabei
mitwandert, misst man am Ende zwei verschiedene Karten und vergleicht die
Zahlen trotzdem. Die Auswahl aendert sich nur hier.

Die Gelaendeform ist bewusst NICHT dabei. Sie kommt aus einem groben
Hoehenmodell und nicht aus OSM-Hoehenlinien -- ihre Kosten sind getrennt zu
messen, sonst vermischen sich zwei ganz verschiedene Datenquellen in einer
Zahl.
"""

# --- 1. Kuestenlinie und Gewaesser ---------------------------------------

WASSER_FLAECHE = {
    ("natural", "water"),
    ("landuse", "reservoir"),
    ("landuse", "basin"),
}

# Fliessgewaesser in zwei Groessen. Ein Fluss gehoert schon auf die
# Uebersichtskarte -- er ist Hindernis und Orientierungslinie zugleich. Ein
# Bach gehoert erst auf die genaue Karte, aber dort unbedingt: er ist die
# haeufigste Trinkwasserquelle ueberhaupt.
WASSER_FLUSS = {
    ("natural", "coastline"),
    ("waterway", "river"),
    ("waterway", "canal"),
}

WASSER_BACH = {
    ("waterway", "stream"),
}

# waterway=ditch und =drain bleiben draussen: in Flachlaendern liegen sie so
# dicht wie ein Strassennetz und fuehren zu keiner Wasserstelle, die man
# trinken wollte.


# --- 3. Bodenbedeckung ----------------------------------------------------
#
# Vier Gruppen, weil die Karte vier Farben braucht: Wald, Offenland, Sumpf,
# Gletscher. Feiner zu unterscheiden kostet Platz und bringt dem Leser nichts
# -- ob eine Wiese eine Weide oder ein Acker ist, aendert im Notfall nichts.

BODEN_WALD = {
    ("natural", "wood"),
    ("landuse", "forest"),
}

BODEN_OFFEN = {
    ("natural", "grassland"),
    ("natural", "heath"),
    ("natural", "scrub"),
    ("natural", "scree"),
    ("natural", "bare_rock"),
    ("natural", "sand"),
    ("natural", "fell"),
    ("landuse", "meadow"),
    ("landuse", "farmland"),
    ("landuse", "grass"),
    ("landuse", "orchard"),
    ("landuse", "vineyard"),
}

BODEN_SUMPF = {
    ("natural", "wetland"),
}

BODEN_GLETSCHER = {
    ("natural", "glacier"),
}


# --- 4. Wege --------------------------------------------------------------
#
# Von der Autobahn bis zum Pfad -- aber in drei Stufen, weil sie zu ganz
# verschiedenen Zeitpunkten auf die Karte gehoeren und darum verschieden viel
# kosten.
#
# WARUM DIE TRENNUNG SEIN MUSS: Auf einer Uebersichtskarte von Europa will
# niemand jeden Waldweg sehen -- die Karte waere schwarz. Auf der genauen
# Karte ist der Pfad dagegen das Wichtigste ueberhaupt. Wer alle Wege in einen
# Topf wirft, kann diese Staffelung nicht messen und muss die teuerste Stufe
# ueberall bezahlen.

WEGE_HAUPT = {
    "motorway", "motorway_link",
    "trunk", "trunk_link",
    "primary", "primary_link",
}

WEGE_NEBEN = {
    "secondary", "secondary_link",
    "tertiary", "tertiary_link",
    "unclassified",
    "residential",
    "living_street",
    "road",
}

WEGE_PFAD = {
    "track",
    "path",
    "bridleway",
    "steps",
}

WEGE_FEIN = {
    "service",
    "footway",
    "cycleway",
    "pedestrian",
    "corridor",
}


# --- 5. Siedlungen als Flaechen ------------------------------------------
#
# Flaechen statt Einzelgebaeuden. Ein Haus einzeln zu zeichnen kostet in
# Oesterreich Millionen von Stuetzpunkten und sagt niemandem, ob dort Menschen
# sind.

SIEDLUNG_FLAECHE = {
    ("landuse", "residential"),
    ("landuse", "industrial"),
    ("landuse", "commercial"),
    ("landuse", "retail"),
    ("landuse", "farmyard"),
    ("landuse", "military"),
}


# --- 6. Punkte ------------------------------------------------------------
#
# Quellen, Brunnen, Huetten, Gipfel, Paesse -- dazu die Ortsnamen, ohne die
# man auf der Karte nicht weiss, wo man ist.

PUNKTE = {
    ("natural", "spring"),
    ("natural", "peak"),
    ("natural", "saddle"),
    ("natural", "cave_entrance"),
    ("man_made", "water_well"),
    ("man_made", "water_tower"),
    ("amenity", "drinking_water"),
    ("amenity", "shelter"),
    ("amenity", "hospital"),
    ("amenity", "pharmacy"),
    ("tourism", "alpine_hut"),
    ("tourism", "wilderness_hut"),
    ("tourism", "viewpoint"),
    ("emergency", "phone"),
}

PUNKTE_FREI = {
    # Schluessel, bei denen jeder Wert zaehlt.
    "mountain_pass",
}

ORTE = {
    ("place", "city"),
    ("place", "town"),
    ("place", "village"),
    ("place", "hamlet"),
    ("place", "isolated_dwelling"),
}


def _trifft(tags, paare):
    for schluessel, wert in paare:
        if tags.get(schluessel) == wert:
            return True
    return False


def sorte_flaeche(tags):
    """Gibt die Sorte einer Flaeche zurueck oder None."""
    if _trifft(tags, WASSER_FLAECHE):
        return "wasser"
    if _trifft(tags, BODEN_GLETSCHER):
        return "gletscher"
    if _trifft(tags, BODEN_SUMPF):
        return "sumpf"
    if _trifft(tags, BODEN_WALD):
        return "wald"
    if _trifft(tags, BODEN_OFFEN):
        return "offen"
    if _trifft(tags, SIEDLUNG_FLAECHE):
        return "siedlung"
    return None


# --- 7. Verwaltungsgrenzen ------------------------------------------------
#
# Nachgetragen am 04.08.2026, nachdem die fertige Uebersichtskarte am Geraet
# zu sehen war: Oesterreich hat keine Kueste, und die Bodenbedeckung faengt
# erst bei Zoomstufe 7 an -- das Land stand ohne Umriss da, nur mit seinen
# Fluessen. Auf einer Karte, die im Ernstfall die Frage "wo bin ich" trifft,
# ist eine Staatsgrenze zudem selbst eine Auskunft.
#
# WARUM ÜBER DIE WEGE UND NICHT ÜBER DIE RELATIONEN: Gezaehlt am 04.08.2026 in
# den Oesterreich-Daten -- 1027 Wege tragen `admin_level=2` direkt, dazu 740
# mit `admin_level=4`. Ueber die Relationen bekaeme man jede Grenze DOPPELT:
# Die deutsch-oesterreichische Linie gehoert zur Aussenlinie beider Laender.
#
# Nur die genauen Werte "2" und "4" zaehlen. In den Daten stehen auch "1ß"
# (acht Wege) und leere Angaben -- Tippfehler, die als eigene Stufe
# durchgingen, wenn man auf "beginnt mit" pruefte.
GRENZSTUFEN = {
    "2": "grenze",
    "4": "grenze-region",
}


def sorte_grenze(tags):
    """Gibt die Sorte einer Verwaltungsgrenze zurueck oder None."""
    if tags.get("boundary") != "administrative":
        return None
    return GRENZSTUFEN.get(tags.get("admin_level"))


def sorte_linie(tags):
    """Gibt die Sorte einer Linie zurueck oder None.

    Die Grenze steht hier NICHT drin -- ein Weg kann beides sein. Ein Fluss,
    der zugleich Staatsgrenze ist, muss als Fluss UND als Grenze auf die
    Karte: Als Grenze sagt er, wo man ist, als Fluss sagt er, wo Wasser ist.
    Wer nur eines von beiden zeichnet, verliert genau an Rhein, Donau, Oder
    und Save die Auskunft, die dort am meisten traegt.
    """
    if _trifft(tags, WASSER_FLUSS):
        return "fluss"
    if _trifft(tags, WASSER_BACH):
        return "bach"
    weg = tags.get("highway")
    if weg in WEGE_HAUPT:
        return "weg-haupt"
    if weg in WEGE_NEBEN:
        return "weg-neben"
    if weg in WEGE_PFAD:
        return "weg-pfad"
    if weg in WEGE_FEIN:
        return "weg-fein"
    return None


# Die Art eines Punktes. Sie steht getrennt von der Sorte, weil alle Punkte
# gleich kodiert werden, aber verschieden aussehen und verschieden wichtig
# sind: Eine Quelle ist im Notfall etwas anderes als ein Aussichtspunkt.
#
# Die Reihenfolge ist die Zahl im Dateiformat und darf nur wachsen, nie
# umsortiert werden -- sonst zeigt eine aeltere Karte ploetzlich Gipfel, wo
# Brunnen stehen.
ARTEN = [
    "unbekannt",
    "quelle", "brunnen", "trinkwasser", "wasserturm",
    "gipfel", "sattel", "pass", "hoehle",
    "huette", "unterstand", "aussicht",
    "krankenhaus", "apotheke", "notruftelefon",
    "grossstadt", "stadt", "dorf", "weiler", "einzellage",
]
ART_NR = {name: i for i, name in enumerate(ARTEN)}

_ART_ZU_TAG = {
    ("natural", "spring"): "quelle",
    ("man_made", "water_well"): "brunnen",
    ("amenity", "drinking_water"): "trinkwasser",
    ("man_made", "water_tower"): "wasserturm",
    ("natural", "peak"): "gipfel",
    ("natural", "saddle"): "sattel",
    ("natural", "cave_entrance"): "hoehle",
    ("tourism", "alpine_hut"): "huette",
    ("tourism", "wilderness_hut"): "huette",
    ("amenity", "shelter"): "unterstand",
    ("tourism", "viewpoint"): "aussicht",
    ("amenity", "hospital"): "krankenhaus",
    ("amenity", "pharmacy"): "apotheke",
    ("emergency", "phone"): "notruftelefon",
    ("place", "city"): "grossstadt",
    ("place", "town"): "stadt",
    ("place", "village"): "dorf",
    ("place", "hamlet"): "weiler",
    ("place", "isolated_dwelling"): "einzellage",
}


def sorte_punkt(tags):
    """Gibt (Sorte, Art) eines Punktes zurueck oder (None, None)."""
    for (schluessel, wert), art in _ART_ZU_TAG.items():
        if tags.get(schluessel) == wert:
            sorte = "ort" if schluessel == "place" else "punkt"
            return sorte, art
    if "mountain_pass" in tags:
        return "punkt", "pass"
    return None, None
