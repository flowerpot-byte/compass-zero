# -*- coding: utf-8 -*-
"""Verzeichnis der Kartengebiete: 250 Eintraege, einer je Gebietscode.

WAS DIESE LISTE IST: ein Verzeichnis von Kartengebieten. Sie sagt, welcher
Auszug bei Geofabrik ein Gebiet enthaelt, wie gross das Gebiet ist und wohin
die Suche im Geraet springt.

WAS SIE NICHT IST: eine Aussage darueber, welche Laender es gibt oder wem ein
Gebiet gehoert. Derselbe Satz steht in `Laender.kt` und gilt hier genauso.
Das Geruest ist ISO 3166-1 alpha-2, weil es eine nachpruefbare, von diesem
Projekt unabhaengige Liste ist -- kein Urteil. Die fuenf nur ausnahmsweise
reservierten Kennzeichen (AC, CP, CQ, DG, TA) fehlen: ihre Gebiete liegen
schon in einem anderen Eintrag.

QUELLEN, alle am 20.08.2026 selbst geladen und im Original gelesen:

  * Geofabrik-Index, `https://download.geofabrik.de/index-v1.json` --
    555 Auszuege mit Namen, Elternteil, Downloadadresse und Umrisspolygon.
    Daraus stammen die Spalten `auszug` und `steckt_in`.
  * Natural Earth 10m, `ne_10m_admin_0_map_units` und
    `ne_10m_admin_0_countries` (gemeinfrei, aus dem Bestand
    `nvkelso/natural-earth-vector`). Daraus stammen die Namen und die
    umschliessenden Rechtecke.
  * Wikidata ueber `query.wikidata.org/sparql`: Gebietscode (P297),
    Flaeche (P2046, auf Quadratmeter genormt und durch eine Million geteilt),
    Hauptstadt (P36) und deren Stelle (P625).

WIE DER AUSZUG BESTIMMT WURDE -- und warum das die heikelste Spalte ist:
Geofabriks eigene Landeskennzeichen sind nicht durchgehend richtig; sechs
Auszuege in Ozeanien tragen ein fremdes Kennzeichen, und Sammelpakete wie
`gcc-states` nennen nicht alle enthaltenen Laender (Saudi-Arabien fehlt dort).
Deshalb wird der eigene Auszug ueber den NAMEN gesucht, den Geofabrik selbst
vergibt, und das Kennzeichen nur genommen, wenn es eindeutig ist. Fuer die
Gebiete ohne eigenen Auszug wurde geometrisch gesucht: von jedem Gebiet bis zu
300 Umrisspunkte gegen jedes Auszugspolygon; der KLEINSTE Auszug, der
mindestens neunzig Hundertstel der Punkte enthaelt, steht in `steckt_in`.

  201 Gebiete haben einen eigenen Auszug.
   47 stecken in einem Sammelpaket -- Saudi-Arabien in `gcc-states`,
      San Marino in `italy`, Gibraltar in `spain/andalucia`, Aaland in
      `finland`, Singapur in `malaysia-singapore-brunei`.
    2 haben ueberhaupt keinen Auszug; was mit ihnen ist, steht im `hinweis`.

Ein Pfad in `auszug` ist nie geraten. Wo keiner belegt ist, steht None, und
`land_bauen.py` bricht dann ab, statt halb zu bauen.

DIE GEGENPROBE, die diese Liste tragen muss: Die 45 Hauptstaedte, die auch in
`Laender.kt` stehen, wurden Zeile fuer Zeile verglichen. 44 stimmen im Namen
und auf drei Kommastellen ueberein; die einzige Abweichung ist Chisinau mit
28,835 statt 28,864 Grad Ost -- drei Kilometer, zwei Stellen in derselben
Stadt.

SPALTEN

    code       ISO 3166-1 alpha-2
    name       deutscher Name
    name_en    englischer Name
    ort        Sprungziel, in der Regel die Hauptstadt
    breite     Breite des Sprungziels in Grad
    laenge     Laenge des Sprungziels in Grad
    flaeche    Flaeche in Quadratkilometern
    auszug     Pfad des eigenen Auszugs bei Geofabrik, sonst None
    steckt_in  Pfad des kleinsten Auszugs, der das Gebiet enthaelt, sonst None
    kasten     umschliessendes Rechteck (West, Sued, Ost, Nord) in Grad
    hinweis    was man ueber dieses Gebiet wissen muss, sonst ""

ZWEI FALLEN IM RECHTECK

1. Bei sieben Gebieten laeuft das Rechteck ueber die Datumsgrenze; dort ist
   WEST GROESSER ALS OST (Fidschi: 174,6 bis -178,2). Wer das nicht prueft,
   holt statt Fidschi den halben Erdball. `ueber_datumsgrenze()` beantwortet
   die Frage, `hoehen_kaesten()` zerlegt so ein Rechteck in zwei.
2. Das Rechteck ist das des GEBIETS, nicht das des Auszugs. Ein Auszug ist in
   der Regel etwas groesser, weil Geofabrik einen Rand mitnimmt, und einige
   Auszuege enthalten mehr als ein Land. Zum Zuschneiden ist das Rechteck des
   Gebiets das richtige; zum Abschaetzen der Dateigroesse taugt es nicht --
   dafuer zaehlt die Groesse der PBF-Datei.

Aufruf zum Nachsehen:
    python tools/karte/laender.py                 Uebersicht und Zahlen
    python tools/karte/laender.py Slowenien       ein Gebiet
    python tools/karte/laender.py --ohne-auszug   was fehlt
"""
import math
import sys
import unicodedata

# Reihenfolge der Spalten wie oben beschrieben.
TABELLE = [
    ("AD", "Andorra", "Andorra", "Andorra la Vella", 42.5072, 1.5222, 468,
     "europe/andorra-latest.osm.pbf", None,
     (1.4065, 42.4287, 1.7651, 42.6494), ""),
    ("AE", "Vereinigte Arabische Emirate", "United Arab Emirates", "Abu Dhabi", 24.4511, 54.3969, 83600,
     None, "asia/gcc-states-latest.osm.pbf",
     (51.5693, 22.6209, 56.3836, 26.0748), ""),
    ("AF", "Afghanistan", "Afghanistan", "Kabul", 34.5328, 69.1658, 652230,
     "asia/afghanistan-latest.osm.pbf", None,
     (60.4868, 29.3866, 74.8923, 38.4737), ""),
    ("AG", "Antigua und Barbuda", "Antigua and Barbuda", "Saint John's", 17.1211, -61.8447, 440.3,
     None, "central-america-latest.osm.pbf",
     (-62.3483, 16.932, -61.6676, 17.7277), "Name der Hauptstadt bei Wikidata nur in einer Sprache belegt (Q36262)"),
    ("AI", "Anguilla", "Anguilla", "The Valley", 18.2208, -63.0517, 91,
     None, "central-america-latest.osm.pbf",
     (-63.4288, 18.1691, -62.9257, 18.6013), ""),
    ("AL", "Albanien", "Albania", "Tirana", 41.3283, 19.8181, 28748,
     "europe/albania-latest.osm.pbf", None,
     (19.272, 39.637, 21.0367, 42.6548), ""),
    ("AM", "Armenien", "Armenia", "Jerewan", 40.1814, 44.5144, 29743,
     "asia/armenia-latest.osm.pbf", None,
     (43.4363, 38.8637, 46.6026, 41.2905), ""),
    ("AO", "Angola", "Angola", "Luanda", -8.8383, 13.2344, 1246700,
     "africa/angola-latest.osm.pbf", None,
     (11.6694, -18.0314, 24.0617, -4.3912), ""),
    ("AQ", "Antarktis", "Antarctica", "Südpol", -90.0, 0.0, 13720000,
     "antarctica-latest.osm.pbf", None,
     (55.1734, -90.0, 54.7122, -60.5162), "das Rechteck umspannt alle Laengengrade; ein Bau am Stueck ist nicht vorgesehen"),
    ("AR", "Argentinien", "Argentina", "Buenos Aires", -34.5997, -58.3819, 2780400,
     "south-america/argentina-latest.osm.pbf", None,
     (-73.5727, -55.052, -53.6616, -21.7869), ""),
    ("AS", "Amerikanisch-Samoa", "American Samoa", "Pago Pago", -14.2794, -170.7006, 199,
     None, "australia-oceania/american-oceania-latest.osm.pbf",
     (-171.0865, -14.5329, -168.1605, -11.0514), ""),
    ("AT", "Österreich", "Austria", "Wien", 48.2083, 16.3725, 83879,
     "europe/austria-latest.osm.pbf", None,
     (9.5212, 46.3786, 17.1483, 49.0098), ""),
    ("AU", "Australien", "Australia", "Canberra", -35.2931, 149.1269, 7692024,
     "australia-oceania/australia-latest.osm.pbf", None,
     (112.9194, -54.7504, 159.1065, -9.2402), ""),
    ("AW", "Aruba", "Aruba", "Oranjestad", 12.5186, -70.0358, 178.9,
     None, "central-america-latest.osm.pbf",
     (-70.0624, 12.4177, -69.8768, 12.6321), ""),
    ("AX", "Åland", "Åland", "Mariehamn", 60.0986, 19.9444, 1583,
     None, "europe/finland-latest.osm.pbf",
     (19.5132, 59.9045, 21.0967, 60.4808), ""),
    ("AZ", "Aserbaidschan", "Azerbaijan", "Baku", 40.3667, 49.8352, 86600,
     "asia/azerbaijan-latest.osm.pbf", None,
     (44.7746, 38.3926, 50.6257, 41.8904), ""),
    ("BA", "Bosnien und Herzegowina", "Bosnia and Herzegovina", "Sarajevo", 43.8564, 18.4131, 51197,
     "europe/bosnia-herzegovina-latest.osm.pbf", None,
     (15.7161, 42.5592, 19.6189, 45.2845), ""),
    ("BB", "Barbados", "Barbados", "Bridgetown", 13.0975, -59.6167, 439,
     None, "central-america-latest.osm.pbf",
     (-59.6542, 13.0512, -59.4269, 13.3446), ""),
    ("BD", "Bangladesch", "Bangladesh", "Dhaka", 23.7289, 90.3944, 147570,
     "asia/bangladesh-latest.osm.pbf", None,
     (88.0218, 20.7387, 92.6429, 26.6235), ""),
    ("BE", "Belgien", "Belgium", "Brüssel", 50.8467, 4.3517, 30688,
     "europe/belgium-latest.osm.pbf", None,
     (2.5218, 49.4952, 6.3745, 51.4962), ""),
    ("BF", "Burkina Faso", "Burkina Faso", "Ouagadougou", 12.3686, -1.5275, 274200,
     "africa/burkina-faso-latest.osm.pbf", None,
     (-5.5226, 9.3919, 2.3902, 15.0799), ""),
    ("BG", "Bulgarien", "Bulgaria", "Sofia", 42.6979, 23.3217, 110994,
     "europe/bulgaria-latest.osm.pbf", None,
     (22.345, 41.2381, 28.6035, 44.2284), ""),
    ("BH", "Bahrain", "Bahrain", "Manama", 26.2167, 50.5833, 786.5,
     None, "asia/gcc-states-latest.osm.pbf",
     (50.3805, 25.5799, 50.8199, 26.2874), ""),
    ("BI", "Burundi", "Burundi", "Gitega", -3.4283, 29.925, 27834,
     "africa/burundi-latest.osm.pbf", None,
     (28.9869, -4.4633, 30.834, -2.3031), ""),
    ("BJ", "Benin", "Benin", "Porto-Novo", 6.4833, 2.6167, 114763,
     "africa/benin-latest.osm.pbf", None,
     (0.7599, 6.2139, 3.8374, 12.3992), ""),
    ("BL", "Saint-Barthélemy", "Saint Barthélemy", "Gustavia", 17.8986, -62.8492, 24,
     None, "central-america-latest.osm.pbf",
     (-62.8673, 17.882, -62.7917, 17.9291), ""),
    ("BM", "Bermuda", "Bermuda", "Hamilton", 32.295, -64.7831, 53,
     "europe/united-kingdom/bermuda-latest.osm.pbf", None,
     (-64.886, 32.2481, -64.6476, 32.3887), ""),
    ("BN", "Brunei", "Brunei", "Bandar Seri Begawan", 4.9167, 114.9167, 5765,
     None, "asia/malaysia-singapore-brunei-latest.osm.pbf",
     (113.9988, 4.0167, 115.3607, 5.0572), ""),
    ("BO", "Bolivien", "Bolivia", "La Paz", -16.4958, -68.1333, 1098581,
     "south-america/bolivia-latest.osm.pbf", None,
     (-69.6665, -22.8973, -57.4657, -9.6798), "weitere eingetragene Hauptstaedte: Sucre"),
    ("BQ", "Karibische Niederlande", "Caribbean Netherlands", "Kralendijk", 12.1508, -68.2767, 322,
     None, "central-america-latest.osm.pbf",
     (-68.4174, 12.022, -62.9451, 17.6469), "keine Hauptstadt; Sprungziel ist Kralendijk auf Bonaire"),
    ("BR", "Brasilien", "Brazil", "Brasília", -15.7939, -47.8828, 8515767,
     "south-america/brazil-latest.osm.pbf", None,
     (-74.0185, -33.7423, -28.8771, 5.2672), ""),
    ("BS", "Bahamas", "The Bahamas", "Nassau", 25.0781, -77.3386, 13878,
     "central-america/bahamas-latest.osm.pbf", None,
     (-79.5944, 20.9124, -72.7462, 26.9284), ""),
    ("BT", "Bhutan", "Bhutan", "Thimphu", 27.4714, 89.6337, 38394,
     "asia/bhutan-latest.osm.pbf", None,
     (88.7301, 26.6961, 92.0888, 28.3584), ""),
    ("BV", "Bouvetinsel", "Bouvet Island", "Bouvetinsel", -54.42, 3.36, 49,
     None, "africa-latest.osm.pbf",
     (3.3456, -54.4625, 3.4867, -54.3801), "unbewohnt; Sprungziel ist die Insel selbst"),
    ("BW", "Botswana", "Botswana", "Gaborone", -24.6569, 25.9086, 581737,
     "africa/botswana-latest.osm.pbf", None,
     (19.9783, -26.8918, 29.3501, -17.7818), ""),
    ("BY", "Belarus", "Belarus", "Minsk", 53.9022, 27.5618, 207597,
     "europe/belarus-latest.osm.pbf", None,
     (23.1656, 51.2352, 32.7195, 56.1568), ""),
    ("BZ", "Belize", "Belize", "Belmopan", 17.25, -88.7675, 22966,
     "central-america/belize-latest.osm.pbf", None,
     (-89.2365, 15.8797, -87.7831, 18.4908), ""),
    ("CA", "Kanada", "Canada", "Ottawa", 45.4247, -75.695, 9984670,
     "north-america/canada-latest.osm.pbf", None,
     (-141.0056, 41.6691, -52.6166, 83.1165), ""),
    ("CC", "Kokosinseln", "Cocos", "West Island", -12.1869, 96.8283, 14,
     "australia-oceania/australia/cocos-islands-latest.osm.pbf", None,
     (96.8215, -12.2, 96.9211, -12.1266), ""),
    ("CD", "Demokratische Republik Kongo", "Democratic Republic of the Congo", "Kinshasa", -4.3219, 15.3119, 2344858,
     "africa/congo-democratic-republic-latest.osm.pbf", None,
     (12.2105, -13.4584, 31.2804, 5.3753), ""),
    ("CF", "Zentralafrikanische Republik", "Central African Republic", "Bangui", 4.3732, 18.5628, 622984,
     "africa/central-african-republic-latest.osm.pbf", None,
     (14.3873, 2.2365, 27.4413, 11.0008), ""),
    ("CG", "Republik Kongo", "Republic of the Congo", "Brazzaville", -4.2694, 15.2711, 342000,
     "africa/congo-brazzaville-latest.osm.pbf", None,
     (11.114, -5.0196, 18.6424, 3.7083), ""),
    ("CH", "Schweiz", "Switzerland", "Bern", 46.948, 7.4474, 41291,
     "europe/switzerland-latest.osm.pbf", None,
     (5.9548, 45.8207, 10.4666, 47.8012), ""),
    ("CI", "Elfenbeinküste", "Ivory Coast", "Yamoussoukro", 6.8161, -5.2742, 322463,
     "africa/ivory-coast-latest.osm.pbf", None,
     (-8.6187, 4.3441, -2.5063, 10.7265), ""),
    ("CK", "Cookinseln", "Cook Islands", "Avarua", -21.207, -159.771, 240,
     "australia-oceania/cook-islands-latest.osm.pbf", None,
     (-165.8245, -21.9389, -157.3128, -8.9467), ""),
    ("CL", "Chile", "Chile", "Santiago de Chile", -33.4375, -70.65, 756102,
     "south-america/chile-latest.osm.pbf", None,
     (-109.4537, -55.9185, -66.4208, -17.5066), ""),
    ("CM", "Kamerun", "Cameroon", "Yaoundé", 3.8578, 11.5181, 475442,
     "africa/cameroon-latest.osm.pbf", None,
     (8.5051, 1.6546, 16.2077, 13.0811), ""),
    ("CN", "China", "People's Republic of China", "Peking", 39.904, 116.4075, 9596961,
     "asia/china-latest.osm.pbf", None,
     (73.6023, 18.1693, 134.7726, 53.5694), ""),
    ("CO", "Kolumbien", "Colombia", "Bogotá", 4.6097, -74.0817, 1141748,
     "south-america/colombia-latest.osm.pbf", None,
     (-81.7237, -4.2365, -66.8751, 13.5784), ""),
    ("CR", "Costa Rica", "Costa Rica", "San José", 9.9327, -84.0796, 51180,
     "central-america/costa-rica-latest.osm.pbf", None,
     (-87.1177, 5.5151, -82.5628, 11.2099), ""),
    ("CU", "Kuba", "Cuba", "Havanna", 23.1367, -82.3589, 109884,
     "central-america/cuba-latest.osm.pbf", None,
     (-84.9496, 19.8278, -74.1329, 23.2656), ""),
    ("CV", "Kap Verde", "Cape Verde", "Praia", 14.9177, -23.5092, 4033,
     "africa/cape-verde-latest.osm.pbf", None,
     (-25.3604, 14.8039, -22.6666, 17.1966), ""),
    ("CW", "Curaçao", "Curaçao", "Willemstad", 12.108, -68.935, 444,
     None, "central-america-latest.osm.pbf",
     (-69.1717, 12.0413, -68.7397, 12.3915), ""),
    ("CX", "Weihnachtsinsel", "Christmas Island", "Flying Fish Cove", -10.4217, 105.6781, 135,
     "australia-oceania/australia/christmas-island-latest.osm.pbf", None,
     (105.5818, -10.566, 105.7147, -10.4308), ""),
    ("CY", "Zypern", "Cyprus", "Nikosia", 35.1725, 33.365, 9242,
     "europe/cyprus-latest.osm.pbf", None,
     (32.2717, 34.625, 34.0991, 35.1871), ""),
    ("CZ", "Tschechien", "Czech Republic", "Prag", 50.0875, 14.4214, 78871,
     "europe/czech-republic-latest.osm.pbf", None,
     (12.0761, 48.5579, 18.8374, 51.04), ""),
    ("DE", "Deutschland", "Germany", "Berlin", 52.5167, 13.3833, 357588,
     "europe/germany-latest.osm.pbf", None,
     (5.8525, 47.2711, 15.0221, 55.0653), ""),
    ("DJ", "Dschibuti", "Djibouti", "Dschibuti", 11.595, 43.1481, 23200,
     "africa/djibouti-latest.osm.pbf", None,
     (41.7491, 10.9298, 43.4187, 12.7079), ""),
    ("DK", "Dänemark", "Denmark", "Kopenhagen", 55.6761, 12.5689, 42925,
     "europe/denmark-latest.osm.pbf", None,
     (8.094, 54.5686, 15.1514, 57.7512), "der Auszug laesst Faeroeer und Groenland aus -- beide haben eigene Auszuege"),
    ("DM", "Dominica", "Dominica", "Roseau", 15.3, -61.3833, 751.1,
     None, "central-america-latest.osm.pbf",
     (-61.4889, 15.2018, -61.2493, 15.6339), ""),
    ("DO", "Dominikanische Republik", "Dominican Republic", "Santo Domingo", 18.4625, -69.9361, 48671,
     None, "central-america/haiti-and-domrep-latest.osm.pbf",
     (-72.0098, 17.5456, -68.3286, 19.9377), ""),
    ("DZ", "Algerien", "Algeria", "Algier", 36.7764, 3.0586, 2381741,
     "africa/algeria-latest.osm.pbf", None,
     (-8.6824, 18.9756, 11.9689, 37.0939), ""),
    ("EC", "Ecuador", "Ecuador", "Quito", -0.22, -78.5125, 257204,
     "south-america/ecuador-latest.osm.pbf", None,
     (-92.0116, -5.0114, -75.2273, 1.6644), ""),
    ("EE", "Estland", "Estonia", "Tallinn", 59.4372, 24.745, 45335,
     "europe/estonia-latest.osm.pbf", None,
     (21.8324, 57.5158, 28.1865, 59.6709), ""),
    ("EG", "Ägypten", "Egypt", "Kairo", 30.0444, 31.2358, 1010408,
     "africa/egypt-latest.osm.pbf", None,
     (24.6883, 21.9944, 36.8992, 31.6565), ""),
    ("EH", "Westsahara", "Western Sahara", "El Aaiún", 27.15, -13.2, 266000,
     None, "africa/morocco-latest.osm.pbf",
     (-17.1046, 20.7669, -8.6808, 27.6615), "umstrittenes Gebiet ohne anerkannte Hauptstadt; Sprungziel ist die groesste Stadt"),
    ("ER", "Eritrea", "Eritrea", "Asmara", 15.3333, 38.9167, 117600,
     "africa/eritrea-latest.osm.pbf", None,
     (36.4236, 12.36, 43.1239, 18.0048), ""),
    ("ES", "Spanien", "Spain", "Madrid", 40.4169, -3.7033, 505990,
     "europe/spain-latest.osm.pbf", None,
     (-18.1672, 27.6422, 4.3371, 43.7934), "der Auszug laesst die Kanaren aus -- die stehen als africa/canary-islands"),
    ("ET", "Äthiopien", "Ethiopia", "Addis Abeba", 9.0358, 38.7525, 1104300,
     "africa/ethiopia-latest.osm.pbf", None,
     (32.9898, 3.4033, 47.9792, 14.8795), ""),
    ("FI", "Finnland", "Finland", "Helsinki", 60.1708, 24.9375, 338478,
     "europe/finland-latest.osm.pbf", None,
     (20.6232, 59.8112, 31.5695, 70.0753), ""),
    ("FJ", "Fidschi", "Fiji", "Suva", -18.1333, 178.4333, 18274,
     "australia-oceania/fiji-latest.osm.pbf", None,
     (174.5889, -21.7111, -178.2161, -12.4753), ""),
    ("FK", "Falklandinseln", "Falkland Islands", "Stanley", -51.6939, -57.8496, 12200,
     "europe/united-kingdom/falklands-latest.osm.pbf", None,
     (-61.3182, -52.4065, -57.7343, -51.0278), ""),
    ("FM", "Föderierte Staaten von Mikronesien", "Federated States of Micronesia", "Palikir", 6.9178, 158.185, 702,
     "australia-oceania/micronesia-latest.osm.pbf", None,
     (138.0638, 0.9182, 163.0466, 9.7756), ""),
    ("FO", "Färöer", "Faroe Islands", "Tórshavn", 62.0098, -6.7713, 1399,
     "europe/faroe-islands-latest.osm.pbf", None,
     (-7.6442, 61.3941, -6.2758, 62.3989), ""),
    ("FR", "Frankreich", "France", "Paris", 48.8567, 2.3522, 643801,
     "europe/france-latest.osm.pbf", None,
     (-5.1328, 41.3659, 9.5596, 51.0875), "Auszug nur fuer das europaeische Frankreich; die Ueberseegebiete haben eigene Auszuege"),
    ("GA", "Gabun", "Gabon", "Libreville", 0.4078, 9.4403, 267667,
     "africa/gabon-latest.osm.pbf", None,
     (8.6956, -3.9369, 14.499, 2.3225), ""),
    ("GB", "Vereinigtes Königreich", "United Kingdom", "London", 51.5072, -0.1275, 242495,
     "europe/united-kingdom-latest.osm.pbf", None,
     (-13.6913, 49.9096, 1.7712, 60.8479), ""),
    ("GD", "Grenada", "Grenada", "St. George’s", 12.0525, -61.7531, 348.5,
     None, "central-america-latest.osm.pbf",
     (-61.7905, 12.0028, -61.4216, 12.5297), ""),
    ("GE", "Georgien", "Georgia", "Tiflis", 41.7225, 44.7925, 69700,
     "europe/georgia-latest.osm.pbf", None,
     (39.986, 41.0441, 46.6948, 43.5758), ""),
    ("GF", "Französisch-Guayana", "French Guiana", "Cayenne", 4.9386, -52.335, 83534,
     "europe/france/guyane-latest.osm.pbf", None,
     (-54.6153, 2.1107, -51.649, 5.7448), ""),
    ("GG", "Guernsey", "Guernsey", "Saint Peter Port", 49.46, -2.5519, 78,
     None, "europe/guernsey-jersey-latest.osm.pbf",
     (-2.6735, 49.4116, -2.1703, 49.7314), ""),
    ("GH", "Ghana", "Ghana", "Accra", 5.556, -0.1969, 238535,
     "africa/ghana-latest.osm.pbf", None,
     (-3.2625, 4.7371, 1.188, 11.1629), ""),
    ("GI", "Gibraltar", "Gibraltar", "Gibraltar", 36.14, -5.35, 6.84,
     None, "europe/spain/andalucia-latest.osm.pbf",
     (-5.3584, 36.1105, -5.3388, 36.1411), ""),
    ("GL", "Grönland", "Greenland", "Nuuk", 64.175, -51.7333, 2166086,
     "north-america/greenland-latest.osm.pbf", None,
     (-73.0572, 59.7926, -11.3768, 83.6341), ""),
    ("GM", "Gambia", "The Gambia", "Banjul", 13.4531, -16.5775, 11300,
     None, "africa/senegal-and-gambia-latest.osm.pbf",
     (-16.8297, 13.065, -13.8187, 13.82), ""),
    ("GN", "Guinea", "Guinea", "Conakry", 9.5092, -13.7122, 245857,
     "africa/guinea-latest.osm.pbf", None,
     (-15.0811, 7.1902, -7.6624, 12.6734), ""),
    ("GP", "Guadeloupe", "Guadeloupe", "Basse-Terre", 15.9969, -61.7328, 1628,
     "europe/france/guadeloupe-latest.osm.pbf", None,
     (-61.7978, 15.847, -60.9892, 16.5131), ""),
    ("GQ", "Äquatorialguinea", "Equatorial Guinea", "Ciudad de la Paz", 1.5917, 10.8222, 28051,
     "africa/equatorial-guinea-latest.osm.pbf", None,
     (5.612, -1.4757, 11.3363, 3.7724), ""),
    ("GR", "Griechenland", "Greece", "Athen", 37.9842, 23.7281, 131957,
     "europe/greece-latest.osm.pbf", None,
     (19.6265, 34.815, 28.2398, 41.7505), ""),
    ("GS", "Südgeorgien und die Südlichen Sandwichinseln", "South Georgia and the South Sandwich Islands", "King Edward Point", -54.2833, -36.4942, 4066,
     None, "south-america-latest.osm.pbf",
     (-38.087, -59.4728, -26.2393, -53.9724), ""),
    ("GT", "Guatemala", "Guatemala", "Guatemala-Stadt", 14.6417, -90.5133, 108889,
     "central-america/guatemala-latest.osm.pbf", None,
     (-92.2463, 13.7314, -88.2209, 17.816), ""),
    ("GU", "Guam", "Guam", "Hagåtña", 13.4833, 144.75, 544,
     None, "australia-oceania/american-oceania-latest.osm.pbf",
     (144.6242, 13.241, 144.9522, 13.6541), ""),
    ("GW", "Guinea-Bissau", "Guinea-Bissau", "Bissau", 11.86, -15.5833, 36125,
     "africa/guinea-bissau-latest.osm.pbf", None,
     (-16.7284, 10.9276, -13.6607, 12.6794), ""),
    ("GY", "Guyana", "Guyana", "Georgetown", 6.8058, -58.1508, 214970,
     "south-america/guyana-latest.osm.pbf", None,
     (-61.3967, 1.1858, -56.4818, 8.558), ""),
    ("HK", "Hongkong", "Hong Kong", "Hongkong", 22.2783, 114.1586, 2755,
     "asia/china/hong-kong-latest.osm.pbf", None,
     (113.8373, 22.1771, 114.4013, 22.5639), "Stadtgebiet, keine Hauptstadt; Sprungziel ist der Stadtpunkt"),
    ("HM", "Heard und McDonaldinseln", "Heard Island and McDonald Islands", "Heard-Insel", -53.0935, 73.517, 368,
     "australia-oceania/australia/heard-mcdonald-latest.osm.pbf", None,
     (73.236, -53.1926, 73.8122, -52.9616), "unbewohnt; Sprungziel ist die Insel selbst"),
    ("HN", "Honduras", "Honduras", "Tegucigalpa", 14.1058, -87.2047, 112492,
     "central-america/honduras-latest.osm.pbf", None,
     (-89.3638, 12.9798, -83.1304, 17.4186), ""),
    ("HR", "Kroatien", "Croatia", "Zagreb", 45.8131, 15.9772, 56594,
     "europe/croatia-latest.osm.pbf", None,
     (13.5015, 42.4163, 19.4078, 46.547), ""),
    ("HT", "Haiti", "Haiti", "Port-au-Prince", 18.5469, -72.3403, 27750,
     None, "central-america/haiti-and-domrep-latest.osm.pbf",
     (-74.4892, 18.0259, -71.6391, 20.0898), ""),
    ("HU", "Ungarn", "Hungary", "Budapest", 47.4983, 19.0408, 93011,
     "europe/hungary-latest.osm.pbf", None,
     (16.094, 45.7413, 22.8776, 48.5692), ""),
    ("ID", "Indonesien", "Indonesia", "Jakarta", -6.1753, 106.8269, 1904570,
     "asia/indonesia-latest.osm.pbf", None,
     (95.0127, -10.9226, 140.9776, 5.9101), "der Auszug enthaelt auch Osttimor"),
    ("IE", "Irland", "Ireland", "Dublin", 53.3497, -6.2603, 69797,
     "europe/ireland-and-northern-ireland-latest.osm.pbf", None,
     (-10.4782, 51.4457, -5.9935, 55.3864), "der Auszug enthaelt auch Nordirland"),
    ("IL", "Israel", "Israel", "Jerusalem", 31.7767, 35.2342, 20770,
     None, "asia/israel-and-palestine-latest.osm.pbf",
     (34.2484, 29.4897, 35.8881, 33.4067), ""),
    ("IM", "Isle of Man", "Isle of Man", "Douglas", 54.15, -4.4775, 572,
     "europe/isle-of-man-latest.osm.pbf", None,
     (-4.7902, 54.057, -4.3119, 54.419), ""),
    ("IN", "Indien", "India", "Neu-Delhi", 28.6139, 77.2089, 3287263,
     "asia/india-latest.osm.pbf", None,
     (68.1434, 6.7456, 97.3623, 35.4954), ""),
    ("IO", "Britisches Territorium im Indischen Ozean", "British Indian Ocean Territory", "Diego Garcia", -7.3133, 72.4111, 60,
     None, "asia-latest.osm.pbf",
     (71.261, -7.4322, 72.4946, -5.227), ""),
    ("IQ", "Irak", "Iraq", "Bagdad", 33.3153, 44.3661, 437072,
     "asia/iraq-latest.osm.pbf", None,
     (38.7745, 29.0631, 48.5593, 37.3755), ""),
    ("IR", "Iran", "Iran", "Teheran", 35.6889, 51.3897, 1648195,
     "asia/iran-latest.osm.pbf", None,
     (44.0149, 25.0594, 63.3196, 39.7715), ""),
    ("IS", "Island", "Iceland", "Reykjavík", 64.1475, -21.935, 103004,
     "europe/iceland-latest.osm.pbf", None,
     (-24.5399, 63.3967, -13.5029, 66.5642), ""),
    ("IT", "Italien", "Italy", "Rom", 41.8931, 12.4828, 302068,
     "europe/italy-latest.osm.pbf", None,
     (6.6027, 35.4892, 18.5174, 47.0852), ""),
    ("JE", "Jersey", "Jersey", "Saint Helier", 49.1858, -2.11, 118.2,
     None, "europe/guernsey-jersey-latest.osm.pbf",
     (-2.242, 49.1713, -2.0083, 49.267), ""),
    ("JM", "Jamaika", "Jamaica", "Kingston", 17.9714, -76.7931, 10992,
     "central-america/jamaica-latest.osm.pbf", None,
     (-78.3747, 17.7032, -76.188, 18.5251), ""),
    ("JO", "Jordanien", "Jordan", "Amman", 31.931, 35.9261, 89341,
     "asia/jordan-latest.osm.pbf", None,
     (34.9494, 29.19, 39.292, 33.3717), ""),
    ("JP", "Japan", "Japan", "Tokio", 35.6894, 139.6917, 377972,
     "asia/japan-latest.osm.pbf", None,
     (122.9382, 24.2121, 153.9856, 45.5204), ""),
    ("KE", "Kenia", "Kenya", "Nairobi", -1.2864, 36.8172, 581309,
     "africa/kenya-latest.osm.pbf", None,
     (33.8905, -4.6775, 41.885, 5.0304), ""),
    ("KG", "Kirgisistan", "Kyrgyzstan", "Bischkek", 42.8667, 74.5667, 199951,
     "asia/kyrgyzstan-latest.osm.pbf", None,
     (69.2263, 39.1892, 80.2576, 43.2617), ""),
    ("KH", "Kambodscha", "Cambodia", "Phnom Penh", 11.5696, 104.921, 181035,
     "asia/cambodia-latest.osm.pbf", None,
     (102.3134, 10.4158, 107.6105, 14.7046), ""),
    ("KI", "Kiribati", "Kiribati", "South Tarawa", 1.3333, 172.9667, 811,
     "australia-oceania/kiribati-latest.osm.pbf", None,
     (169.5221, -11.4611, -151.781, 4.7231), ""),
    ("KM", "Komoren", "Comoros", "Moroni", -11.7036, 43.2536, 2034,
     "africa/comores-latest.osm.pbf", None,
     (43.2132, -12.3803, 44.5291, -11.3613), ""),
    ("KN", "St. Kitts und Nevis", "Saint Kitts and Nevis", "Basseterre", 17.2983, -62.7342, 269.4,
     None, "central-america-latest.osm.pbf",
     (-62.8611, 17.1005, -62.5368, 17.4158), ""),
    ("KP", "Nordkorea", "North Korea", "Pjöngjang", 39.0167, 125.7475, 120540,
     "asia/north-korea-latest.osm.pbf", None,
     (124.2113, 37.6756, 130.7, 43.0103), ""),
    ("KR", "Südkorea", "South Korea", "Seoul", 37.56, 126.99, 100295,
     "asia/south-korea-latest.osm.pbf", None,
     (124.6136, 33.1976, 131.8625, 38.5781), ""),
    ("KW", "Kuwait", "Kuwait", "Kuweit-Stadt", 29.375, 47.98, 17818,
     None, "asia/gcc-states-latest.osm.pbf",
     (46.5324, 28.5335, 48.4328, 30.0982), ""),
    ("KY", "Cayman Islands", "Cayman Islands", "George Town", 19.2964, -81.3817, 264,
     None, "central-america-latest.osm.pbf",
     (-81.4165, 19.2639, -79.7266, 19.7576), ""),
    ("KZ", "Kasachstan", "Kazakhstan", "Astana", 51.1333, 71.4333, 2724900,
     "asia/kazakhstan-latest.osm.pbf", None,
     (46.4783, 40.5847, 87.3238, 55.4346), ""),
    ("LA", "Laos", "Laos", "Vientiane", 17.98, 102.63, 236800,
     "asia/laos-latest.osm.pbf", None,
     (100.0971, 13.9155, 107.6644, 22.496), ""),
    ("LB", "Libanon", "Lebanon", "Beirut", 33.8869, 35.5131, 10452,
     "asia/lebanon-latest.osm.pbf", None,
     (35.0996, 33.0556, 36.6041, 34.6875), ""),
    ("LC", "St. Lucia", "Saint Lucia", "Castries", 14.0108, -60.9894, 617,
     None, "central-america-latest.osm.pbf",
     (-61.0785, 13.7147, -60.883, 14.1119), ""),
    ("LI", "Liechtenstein", "Liechtenstein", "Vaduz", 47.1406, 9.5222, 160,
     "europe/liechtenstein-latest.osm.pbf", None,
     (9.4759, 47.0524, 9.6157, 47.2628), ""),
    ("LK", "Sri Lanka", "Sri Lanka", "Colombo", 6.9267, 79.8606, 65610,
     "asia/sri-lanka-latest.osm.pbf", None,
     (79.6558, 5.9237, 81.8903, 9.8296), "weitere eingetragene Hauptstaedte: Sri Jayewardenepura Kotte"),
    ("LR", "Liberia", "Liberia", "Monrovia", 6.3106, -10.8047, 111369,
     "africa/liberia-latest.osm.pbf", None,
     (-11.4762, 4.3472, -7.3841, 8.5654), ""),
    ("LS", "Lesotho", "Lesotho", "Maseru", -29.31, 27.48, 30355,
     "africa/lesotho-latest.osm.pbf", None,
     (27.0022, -30.6588, 29.4359, -28.5708), ""),
    ("LT", "Litauen", "Lithuania", "Vilnius", 54.6872, 25.28, 65300,
     "europe/lithuania-latest.osm.pbf", None,
     (20.9246, 53.8868, 26.8007, 56.4426), ""),
    ("LU", "Luxemburg", "Luxembourg", "Luxemburg", 49.6114, 6.13, 2593,
     "europe/luxembourg-latest.osm.pbf", None,
     (5.7149, 49.4413, 6.5026, 50.175), ""),
    ("LV", "Lettland", "Latvia", "Riga", 56.9475, 24.1069, 64594,
     "europe/latvia-latest.osm.pbf", None,
     (20.9686, 55.667, 28.2173, 58.0751), ""),
    ("LY", "Libyen", "Libya", "Tripolis", 32.8875, 13.1875, 1759541,
     "africa/libya-latest.osm.pbf", None,
     (9.2865, 19.4961, 25.1563, 33.1812), ""),
    ("MA", "Marokko", "Morocco", "Rabat", 34.0211, -6.8414, 446550,
     "africa/morocco-latest.osm.pbf", None,
     (-17.0137, 21.42, -1.032, 35.9265), ""),
    ("MC", "Monaco", "Monaco", "Monaco", 43.7311, 7.42, 2.08,
     "europe/monaco-latest.osm.pbf", None,
     (7.3658, 43.718, 7.4375, 43.7635), ""),
    ("MD", "Moldau", "Moldova", "Chișinău", 47.0228, 28.8353, 33844,
     "europe/moldova-latest.osm.pbf", None,
     (26.6179, 45.4618, 30.1316, 48.486), ""),
    ("ME", "Montenegro", "Montenegro", "Podgorica", 42.4414, 19.2628, 13883,
     "europe/montenegro-latest.osm.pbf", None,
     (18.4335, 41.8524, 20.3552, 43.5479), ""),
    ("MF", "Saint-Martin", "Saint Martin", "Marigot", 18.0667, -63.0847, 53.2,
     None, "central-america-latest.osm.pbf",
     (-63.1468, 18.0334, -63.0107, 18.1221), ""),
    ("MG", "Madagaskar", "Madagascar", "Antananarivo", -18.91, 47.525, 587295,
     "africa/madagascar-latest.osm.pbf", None,
     (43.2229, -25.5986, 50.5039, -11.9436), ""),
    ("MH", "Marshallinseln", "Marshall Islands", "Majuro", 7.0918, 171.3802, 181.4,
     "australia-oceania/marshall-islands-latest.osm.pbf", None,
     (165.2822, 4.5738, 172.0298, 14.6105), ""),
    ("MK", "Nordmazedonien", "North Macedonia", "Skopje", 41.9961, 21.4317, 25713,
     "europe/macedonia-latest.osm.pbf", None,
     (20.4442, 40.8494, 23.0096, 42.3703), ""),
    ("ML", "Mali", "Mali", "Bamako", 12.6458, -7.9922, 1240192,
     "africa/mali-latest.osm.pbf", None,
     (-12.2641, 10.1401, 4.2356, 24.9951), ""),
    ("MM", "Myanmar", "Myanmar", "Naypyidaw", 19.7475, 96.115, 676577,
     "asia/myanmar-latest.osm.pbf", None,
     (92.175, 9.7907, 101.1739, 28.5385), ""),
    ("MN", "Mongolei", "Mongolia", "Ulaanbaatar", 47.9214, 106.9055, 1564116,
     "asia/mongolia-latest.osm.pbf", None,
     (87.7357, 41.5861, 119.907, 52.1296), ""),
    ("MO", "Macau", "Macau", "Macau", 22.19, 113.5381, 115.3,
     "asia/china/macau-latest.osm.pbf", None,
     (113.5199, 22.1054, 113.5875, 22.2208), "Stadtgebiet, keine Hauptstadt; Sprungziel ist der Stadtpunkt"),
    ("MP", "Nördliche Marianen", "Northern Mariana Islands", "Saipan", 15.2123, 145.7545, 464,
     None, "australia-oceania/american-oceania-latest.osm.pbf",
     (144.9021, 14.1107, 145.8689, 20.5554), ""),
    ("MQ", "Martinique", "Martinique", "Fort-de-France", 14.6, -61.0667, 1128,
     "europe/france/martinique-latest.osm.pbf", None,
     (-61.2288, 14.4081, -60.8104, 14.8769), ""),
    ("MR", "Mauretanien", "Mauritania", "Nouakchott", 18.0858, -15.9785, 1030700,
     "africa/mauritania-latest.osm.pbf", None,
     (-17.0812, 14.7344, -4.8216, 27.2854), ""),
    ("MS", "Montserrat", "Montserrat", "Brades", 16.7928, -62.2106, 102,
     None, "central-america-latest.osm.pbf",
     (-62.2301, 16.6754, -62.1405, 16.8193), "weitere eingetragene Hauptstaedte: Plymouth"),
    ("MT", "Malta", "Malta", "Valletta", 35.8978, 14.5125, 316,
     "europe/malta-latest.osm.pbf", None,
     (14.1836, 35.8012, 14.5671, 36.0756), ""),
    ("MU", "Mauritius", "Mauritius", "Port Louis", -20.1619, 57.4989, 2040,
     "africa/mauritius-latest.osm.pbf", None,
     (56.5242, -20.5173, 63.4939, -10.3239), ""),
    ("MV", "Malediven", "Maldives", "Malé", 4.175, 73.5083, 298,
     "asia/maldives-latest.osm.pbf", None,
     (72.6848, -0.6886, 73.7532, 7.1072), ""),
    ("MW", "Malawi", "Malawi", "Lilongwe", -13.9864, 33.7681, 118484,
     "africa/malawi-latest.osm.pbf", None,
     (32.6633, -17.1353, 35.9043, -9.3812), ""),
    ("MX", "Mexiko", "Mexico", "Mexiko-Stadt", 19.3538, -99.1359, 1972550,
     "north-america/mexico-latest.osm.pbf", None,
     (-118.3688, 14.5463, -86.7006, 32.7128), ""),
    ("MY", "Malaysia", "Malaysia", "Kuala Lumpur", 3.1478, 101.6953, 330803,
     "asia/malaysia-singapore-brunei-latest.osm.pbf", None,
     (99.6452, 0.8514, 119.2781, 7.3558), "der Auszug enthaelt auch Singapur und Brunei"),
    ("MZ", "Mosambik", "Mozambique", "Maputo", -25.9689, 32.5733, 801590,
     "africa/mozambique-latest.osm.pbf", None,
     (30.2138, -26.8603, 40.848, -10.469), ""),
    ("NA", "Namibia", "Namibia", "Windhoek", -22.57, 17.0836, 825615,
     "africa/namibia-latest.osm.pbf", None,
     (11.7176, -28.9594, 25.2598, -16.9511), ""),
    ("NC", "Neukaledonien", "New Caledonia", "Nouméa", -22.2667, 166.45, 18576,
     "australia-oceania/new-caledonia-latest.osm.pbf", None,
     (163.6157, -22.6707, 171.3438, -19.6237), ""),
    ("NE", "Niger", "Niger", "Niamey", 13.515, 2.1175, 1267000,
     "africa/niger-latest.osm.pbf", None,
     (0.1529, 11.6958, 15.9703, 23.5174), ""),
    ("NF", "Norfolkinsel", "Norfolk Island", "Kingston", -29.05, 167.9667, 34.6,
     "australia-oceania/australia/norfolk-island-latest.osm.pbf", None,
     (167.9121, -29.08, 167.9963, -28.9975), ""),
    ("NG", "Nigeria", "Nigeria", "Abuja", 9.0556, 7.4914, 923768,
     "africa/nigeria-latest.osm.pbf", None,
     (2.6711, 4.2722, 14.6699, 13.8803), ""),
    ("NI", "Nicaragua", "Nicaragua", "Managua", 12.1547, -86.2737, 120340,
     "central-america/nicaragua-latest.osm.pbf", None,
     (-87.6858, 10.7135, -82.7257, 15.031), ""),
    ("NL", "Niederlande", "Netherlands", "Amsterdam", 52.3667, 4.8833, 42201,
     "europe/netherlands-latest.osm.pbf", None,
     (3.3494, 50.7475, 7.1985, 53.5581), "Auszug nur fuer den europaeischen Teil; die karibischen Inseln stecken in central-america"),
    ("NO", "Norwegen", "Norway", "Oslo", 59.9133, 10.7389, 385207,
     "europe/norway-latest.osm.pbf", None,
     (4.6438, 57.9932, 31.077, 71.1653), "der Auszug laesst Svalbard und Jan Mayen aus -- die stehen als europe/norway/svalbard-janmayen"),
    ("NP", "Nepal", "Nepal", "Kathmandu", 27.71, 85.32, 147181,
     "asia/nepal-latest.osm.pbf", None,
     (80.0303, 26.3438, 88.1691, 30.4169), ""),
    ("NR", "Nauru", "Nauru", "Yaren", -0.5477, 166.9209, 21,
     "australia-oceania/nauru-latest.osm.pbf", None,
     (166.907, -0.5519, 166.9583, -0.4904), ""),
    ("NU", "Niue", "Niue", "Alofi", -19.056, -169.921, 260,
     "australia-oceania/niue-latest.osm.pbf", None,
     (-169.9504, -19.1428, -169.7829, -18.964), ""),
    ("NZ", "Neuseeland", "New Zealand", "Wellington", -41.2889, 174.7772, 268021,
     "australia-oceania/new-zealand-latest.osm.pbf", None,
     (165.8865, -52.6003, -176.1139, -29.2219), ""),
    ("OM", "Oman", "Oman", "Maskat", 23.6139, 58.5922, 309500,
     None, "asia/gcc-states-latest.osm.pbf",
     (51.9786, 16.6424, 59.8446, 26.386), ""),
    ("PA", "Panama", "Panama", "Panama-Stadt", 8.9711, -79.5347, 74177,
     "central-america/panama-latest.osm.pbf", None,
     (-83.0532, 7.2057, -77.1633, 9.6293), ""),
    ("PE", "Peru", "Peru", "Lima", -12.06, -77.0375, 1285216,
     "south-america/peru-latest.osm.pbf", None,
     (-81.3376, -18.3377, -68.6843, -0.0291), ""),
    ("PF", "Französisch-Polynesien", "French Polynesia", "Papeete", -17.5397, -149.5689, 4167,
     "australia-oceania/polynesie-francaise-latest.osm.pbf", None,
     (-154.537, -27.6412, -134.943, -7.9501), ""),
    ("PG", "Papua-Neuguinea", "Papua New Guinea", "Port Moresby", -9.4789, 147.1494, 462840,
     "australia-oceania/papua-new-guinea-latest.osm.pbf", None,
     (140.8492, -11.6363, 155.9675, -1.3464), ""),
    ("PH", "Philippinen", "Philippines", "Manila", 14.5958, 120.9772, 343448,
     "asia/philippines-latest.osm.pbf", None,
     (116.9549, 4.6557, 126.6177, 21.1224), ""),
    ("PK", "Pakistan", "Pakistan", "Islamabad", 33.6989, 73.0369, 881913,
     "asia/pakistan-latest.osm.pbf", None,
     (60.8444, 23.6945, 77.049, 37.0545), ""),
    ("PL", "Polen", "Poland", "Warschau", 52.23, 21.0111, 312683,
     "europe/poland-latest.osm.pbf", None,
     (14.1239, 48.994, 24.1432, 54.8383), ""),
    ("PM", "Saint-Pierre und Miquelon", "Saint Pierre and Miquelon", "Saint-Pierre", 46.7778, -56.1778, 242,
     None, "north-america/canada-latest.osm.pbf",
     (-56.3966, 46.7528, -56.1448, 47.1413), ""),
    ("PN", "Pitcairninseln", "Pitcairn Islands", "Adamstown", -25.0667, -130.1, 47,
     "australia-oceania/pitcairn-islands-latest.osm.pbf", None,
     (-130.7531, -25.0771, -124.7781, -23.9244), ""),
    ("PR", "Puerto Rico", "Puerto Rico", "San Juan", 18.4653, -66.1167, 9104,
     "north-america/us/puerto-rico-latest.osm.pbf", None,
     (-67.9378, 17.9229, -65.2446, 18.5228), ""),
    ("PS", "Palästina", "Palestine", "Ostjerusalem", 31.7834, 35.2339, 6020,
     None, "asia/israel-and-palestine-latest.osm.pbf",
     (34.2003, 31.2114, 35.5725, 32.5426), "weitere eingetragene Hauptstaedte: Ramallah"),
    ("PT", "Portugal", "Portugal", "Lissabon", 38.708, -9.139, 92225,
     "europe/portugal-latest.osm.pbf", None,
     (-31.2849, 30.0292, -6.2059, 42.1536), "der Auszug laesst die Azoren aus -- die stehen als europe/azores"),
    ("PW", "Palau", "Palau", "Ngerulmud", 7.5006, 134.6242, 465.6,
     "australia-oceania/palau-latest.osm.pbf", None,
     (131.1311, 2.949, 134.7273, 8.0966), ""),
    ("PY", "Paraguay", "Paraguay", "Asunción", -25.28, -57.6344, 406756,
     "south-america/paraguay-latest.osm.pbf", None,
     (-62.6504, -27.5868, -54.2453, -19.2867), ""),
    ("QA", "Katar", "Qatar", "Doha", 25.2861, 51.5294, 11437,
     None, "asia/gcc-states-latest.osm.pbf",
     (50.751, 24.5599, 51.6165, 26.1601), ""),
    ("RE", "Réunion", "Réunion", "Saint-Denis", -20.8789, 55.4481, 2512,
     "europe/france/reunion-latest.osm.pbf", None,
     (55.2254, -21.3708, 55.8545, -20.8614), ""),
    ("RO", "Rumänien", "Romania", "Bukarest", 44.4134, 26.0978, 238397,
     "europe/romania-latest.osm.pbf", None,
     (20.2428, 43.65, 29.6996, 48.2748), ""),
    ("RS", "Serbien", "Serbia", "Belgrad", 44.8178, 20.4569, 88499,
     "europe/serbia-latest.osm.pbf", None,
     (18.845, 42.2349, 22.9846, 46.1739), ""),
    ("RU", "Russland", "Russia", "Moskau", 55.7506, 37.6175, 17075400,
     "russia-latest.osm.pbf", None,
     (19.6095, 41.1927, -168.9946, 81.8587), ""),
    ("RW", "Ruanda", "Rwanda", "Kigali", -1.9525, 30.115, 26338,
     "africa/rwanda-latest.osm.pbf", None,
     (28.8572, -2.8269, 30.8878, -1.0587), ""),
    ("SA", "Saudi-Arabien", "Saudi Arabia", "Riad", 24.65, 46.71, 2250000,
     None, "asia/gcc-states-latest.osm.pbf",
     (34.5728, 16.371, 55.6376, 32.1213), ""),
    ("SB", "Salomonen", "Solomon Islands", "Honiara", -9.4333, 159.95, 28400,
     "australia-oceania/solomon-islands-latest.osm.pbf", None,
     (155.508, -12.2906, 168.8259, -6.5999), ""),
    ("SC", "Seychellen", "Seychelles", "Victoria", -4.6236, 55.4544, 459,
     "africa/seychelles-latest.osm.pbf", None,
     (46.2074, -9.7555, 56.2874, -3.7911), ""),
    ("SD", "Sudan", "Sudan", "Chartum", 15.6031, 32.5265, 1886068,
     "africa/sudan-latest.osm.pbf", None,
     (21.8094, 8.6816, 38.6039, 22.227), ""),
    ("SE", "Schweden", "Sweden", "Stockholm", 59.3294, 18.0686, 447425,
     "europe/sweden-latest.osm.pbf", None,
     (11.1082, 55.3427, 24.1634, 69.0364), ""),
    ("SG", "Singapur", "Singapore", "Singapur", 1.3, 103.8, 719.1,
     None, "asia/malaysia-singapore-brunei-latest.osm.pbf",
     (103.6404, 1.2643, 104.0034, 1.4486), ""),
    ("SH", "St. Helena", "Saint Helena", "Jamestown", -15.9251, -5.7179, 394,
     "africa/saint-helena-ascension-and-tristan-da-cunha-latest.osm.pbf", None,
     (-14.4177, -40.3979, -5.6504, -7.8779), ""),
    ("SI", "Slowenien", "Slovenia", "Ljubljana", 46.0514, 14.5061, 20271,
     "europe/slovenia-latest.osm.pbf", None,
     (13.3653, 45.4236, 16.5153, 46.864), ""),
    ("SJ", "Svalbard und Jan Mayen", "Svalbard", "Longyearbyen", 78.2167, 15.6333, 61399,
     "europe/norway/svalbard-janmayen-latest.osm.pbf", None,
     (10.4844, 74.3474, 33.6404, 80.7701), "keine Hauptstadt; Sprungziel ist der Verwaltungssitz Longyearbyen"),
    ("SK", "Slowakei", "Slovakia", "Bratislava", 48.1517, 17.1093, 49034,
     "europe/slovakia-latest.osm.pbf", None,
     (16.8445, 47.75, 22.5396, 49.6018), ""),
    ("SL", "Sierra Leone", "Sierra Leone", "Freetown", 8.4872, -13.2356, 71740,
     "africa/sierra-leone-latest.osm.pbf", None,
     (-13.3011, 6.9194, -10.2822, 9.996), ""),
    ("SM", "San Marino", "San Marino", "San Marino", 43.932, 12.4484, 61.2,
     None, "europe/italy-latest.osm.pbf",
     (12.3856, 43.8921, 12.4924, 43.9826), ""),
    ("SN", "Senegal", "Senegal", "Dakar", 14.6726, -17.432, 196722,
     None, "africa/senegal-and-gambia-latest.osm.pbf",
     (-17.536, 12.3056, -11.3778, 16.6914), ""),
    ("SO", "Somalia", "Somalia", "Mogadischu", 2.0392, 45.3419, 637657,
     "africa/somalia-latest.osm.pbf", None,
     (40.9654, -1.6963, 51.417, 11.9891), ""),
    ("SR", "Suriname", "Suriname", "Paramaribo", 5.8667, -55.1667, 163270,
     "south-america/suriname-latest.osm.pbf", None,
     (-58.0677, 1.8335, -53.9864, 6.0116), ""),
    ("SS", "Südsudan", "South Sudan", "Juba", 4.8539, 31.5825, 644329,
     "africa/south-sudan-latest.osm.pbf", None,
     (24.1216, 3.4902, 35.9208, 12.2162), ""),
    ("ST", "São Tomé und Príncipe", "São Tomé and Príncipe", "São Tomé", 0.3375, 6.7283, 1001,
     "africa/sao-tome-and-principe-latest.osm.pbf", None,
     (6.4617, 0.0241, 7.4627, 1.6998), ""),
    ("SV", "El Salvador", "El Salvador", "San Salvador", 13.6976, -89.1912, 21041,
     "central-america/el-salvador-latest.osm.pbf", None,
     (-90.1148, 13.1586, -87.6932, 14.4454), ""),
    ("SX", "Sint Maarten", "Sint Maarten", "Philipsburg", 18.0242, -63.0433, 34,
     None, "central-america-latest.osm.pbf",
     (-63.1189, 18.0191, -63.0176, 18.0621), ""),
    ("SY", "Syrien", "Syria", "Damaskus", 33.513, 36.292, 185180,
     "asia/syria-latest.osm.pbf", None,
     (35.7234, 32.313, 42.3772, 37.3249), ""),
    ("SZ", "Eswatini", "Eswatini", "Lobamba", -26.4465, 31.2064, 17364,
     "africa/swaziland-latest.osm.pbf", None,
     (30.7829, -27.3163, 32.1174, -25.736), "weitere eingetragene Hauptstaedte: Mbabane"),
    ("TC", "Turks- und Caicosinseln", "Turks and Caicos Islands", "Cockburn Town", 21.4603, -71.1414, 417,
     None, "central-america-latest.osm.pbf",
     (-72.4813, 21.2901, -71.1289, 21.9592), ""),
    ("TD", "Tschad", "Chad", "N’Djamena", 12.11, 15.05, 1284000,
     "africa/chad-latest.osm.pbf", None,
     (13.4492, 7.4556, 23.9844, 23.4447), ""),
    ("TF", "Französische Süd- und Antarktisgebiete", "French Southern and Antarctic Lands", "Port-aux-Français", -49.35, 70.2167, 7829,
     None, None,
     (39.7283, -49.7216, 77.5852, -11.5506), "kein Auszug deckt das ganze Gebiet: die Inseln im Indischen Ozean liegen in australia-oceania, Adelieland in antarctica"),
    ("TG", "Togo", "Togo", "Lomé", 6.13, 1.2158, 56785,
     "africa/togo-latest.osm.pbf", None,
     (-0.1661, 6.1005, 1.7824, 11.135), ""),
    ("TH", "Thailand", "Thailand", "Bangkok", 13.75, 100.5167, 513120,
     "asia/thailand-latest.osm.pbf", None,
     (97.3514, 5.6299, 105.651, 20.445), ""),
    ("TJ", "Tadschikistan", "Tajikistan", "Duschanbe", 38.5731, 68.7864, 143100,
     "asia/tajikistan-latest.osm.pbf", None,
     (67.3427, 36.6786, 75.1641, 41.04), ""),
    ("TK", "Tokelau", "Tokelau", "Tokelau", -9.1667, -171.8333, 10,
     "australia-oceania/tokelau-latest.osm.pbf", None,
     (-172.5026, -9.3617, -171.1857, -8.5432), "keine Hauptstadt; der Verwaltungssitz wechselt jaehrlich zwischen den drei Atollen"),
    ("TL", "Osttimor", "East Timor", "Dili", -8.5536, 125.5783, 14919,
     "asia/east-timor-latest.osm.pbf", None,
     (124.03, -9.5012, 127.3132, -8.135), ""),
    ("TM", "Turkmenistan", "Turkmenistan", "Aşgabat", 37.95, 58.3833, 491210,
     "asia/turkmenistan-latest.osm.pbf", None,
     (52.4377, 35.1406, 66.6458, 42.7912), ""),
    ("TN", "Tunesien", "Tunisia", "Tunis", 36.8008, 10.18, 163610,
     "africa/tunisia-latest.osm.pbf", None,
     (7.4798, 30.2289, 11.5641, 37.3452), ""),
    ("TO", "Tonga", "Tonga", "Nukuʻalofa", -21.1343, -175.2018, 748.5,
     "australia-oceania/tonga-latest.osm.pbf", None,
     (-176.2193, -22.3388, -173.9143, -15.5595), ""),
    ("TR", "Türkei", "Turkey", "Ankara", 39.9358, 32.8387, 783562,
     "europe/turkey-latest.osm.pbf", None,
     (25.6633, 35.8198, 44.807, 42.0988), ""),
    ("TT", "Trinidad und Tobago", "Trinidad and Tobago", "Port of Spain", 10.6667, -61.5167, 5128,
     None, "central-america-latest.osm.pbf",
     (-61.9287, 10.0421, -60.5221, 11.3511), ""),
    ("TV", "Tuvalu", "Tuvalu", "Funafuti", -8.5048, 179.1174, 25.1,
     "australia-oceania/tuvalu-latest.osm.pbf", None,
     (176.1253, -9.4207, 179.9067, -5.6775), ""),
    ("TW", "Taiwan", "Taiwan", "Taipeh", 25.0375, 121.5625, 36193,
     "asia/taiwan-latest.osm.pbf", None,
     (118.2796, 21.9046, 122.0054, 25.2874), ""),
    ("TZ", "Tansania", "Tanzania", "Dodoma", -6.1835, 35.746, 947303,
     "africa/tanzania-latest.osm.pbf", None,
     (29.321, -11.7313, 40.4494, -0.9858), ""),
    ("UA", "Ukraine", "Ukraine", "Kiew", 50.45, 30.5236, 603550,
     "europe/ukraine-latest.osm.pbf", None,
     (22.1328, 45.2136, 40.1595, 52.3689), "der Auszug enthaelt die Krim"),
    ("UG", "Uganda", "Uganda", "Kampala", 0.3136, 32.5811, 241038,
     "africa/uganda-latest.osm.pbf", None,
     (29.5485, -1.4752, 35.0065, 4.2197), ""),
    ("UM", "Kleinere Amerikanische Überseeinseln", "United States Minor Outlying Islands", "Wake", 19.3, 166.6333, 34.2,
     None, None,
     (166.6194, -0.3888, -75.0026, 28.2153), "unbewohnte Streuinseln in zwei Ozeanen; die Pazifikinseln stecken in american-oceania, Navassa in central-america"),
    ("US", "Vereinigte Staaten", "United States of America", "Washington, D.C.", 38.895, -77.0367, 9826675,
     "north-america/us-latest.osm.pbf", None,
     (172.4761, 18.9061, -66.9773, 71.4125), ""),
    ("UY", "Uruguay", "Uruguay", "Montevideo", -34.9059, -56.1913, 176215,
     "south-america/uruguay-latest.osm.pbf", None,
     (-58.4394, -34.9734, -53.1108, -30.0969), ""),
    ("UZ", "Usbekistan", "Uzbekistan", "Taschkent", 41.3111, 69.2797, 448978,
     "asia/uzbekistan-latest.osm.pbf", None,
     (55.9758, 37.1851, 73.1486, 45.5587), ""),
    ("VA", "Vatikanstadt", "Vatican City", "Vatikanstadt", 41.904, 12.453, 0.49,
     None, "europe/italy/centro-latest.osm.pbf",
     (12.4527, 41.9028, 12.454, 41.9039), ""),
    ("VC", "St. Vincent und die Grenadinen", "Saint Vincent and the Grenadines", "Kingstown", 13.1553, -61.2274, 389,
     None, "central-america-latest.osm.pbf",
     (-61.4598, 12.5852, -61.1239, 13.3808), ""),
    ("VE", "Venezuela", "Venezuela", "Caracas", 10.5061, -66.9144, 912050,
     "south-america/venezuela-latest.osm.pbf", None,
     (-73.3911, 0.6493, -59.8156, 15.7029), ""),
    ("VG", "Britische Jungferninseln", "British Virgin Islands", "Road Town", 18.4333, -64.6167, 151,
     None, "central-america-latest.osm.pbf",
     (-64.774, 18.3347, -64.2707, 18.7462), ""),
    ("VI", "Amerikanische Jungferninseln", "United States Virgin Islands", "Charlotte Amalie", 18.35, -64.95, 346.4,
     "north-america/us/us-virgin-islands-latest.osm.pbf", None,
     (-65.0415, 17.6828, -64.5594, 18.3866), ""),
    ("VN", "Vietnam", "Vietnam", "Hanoi", 21.0245, 105.8412, 331690,
     "asia/vietnam-latest.osm.pbf", None,
     (102.1187, 8.5656, 109.4724, 23.3663), ""),
    ("VU", "Vanuatu", "Vanuatu", "Port Vila", -17.7333, 168.3167, 12190,
     "australia-oceania/vanuatu-latest.osm.pbf", None,
     (166.5205, -20.2531, 169.8989, -13.0649), ""),
    ("WF", "Wallis und Futuna", "Wallis and Futuna", "Mata-Utu", -13.2827, -176.1737, 274,
     "australia-oceania/wallis-et-futuna-latest.osm.pbf", None,
     (-178.1857, -14.3194, -176.1256, -13.2089), ""),
    ("WS", "Samoa", "Samoa", "Apia", -13.8333, -171.8333, 2842,
     "australia-oceania/samoa-latest.osm.pbf", None,
     (-172.7826, -14.0528, -171.4377, -13.4628), ""),
    ("XK", "Kosovo", "Kosovo", "Pristina", 42.6667, 21.1667, 10909,
     "europe/kosovo-latest.osm.pbf", None,
     (20.0248, 41.844, 21.7728, 43.2631), ""),
    ("YE", "Jemen", "Yemen", "Aden", 12.8, 45.0333, 455503,
     "asia/yemen-latest.osm.pbf", None,
     (42.5457, 12.1114, 54.5403, 18.9956), "weitere eingetragene Hauptstaedte: Sanaa"),
    ("YT", "Mayotte", "Mayotte", "Mamoudzou", -12.7814, 45.2317, 374,
     "europe/france/mayotte-latest.osm.pbf", None,
     (45.0425, -12.9892, 45.2909, -12.6472), ""),
    ("ZA", "Südafrika", "South Africa", "Pretoria", -25.7464, 28.1881, 1221037,
     "africa/south-africa-latest.osm.pbf", None,
     (16.47, -46.9658, 37.9778, -22.1265), "Regierungssitz als Sprungziel; Parlament in Kapstadt, oberstes Gericht in Bloemfontein"),
    ("ZM", "Sambia", "Zambia", "Lusaka", -15.4167, 28.2833, 752618,
     "africa/zambia-latest.osm.pbf", None,
     (21.9799, -18.0692, 33.6742, -8.1941), ""),
    ("ZW", "Simbabwe", "Zimbabwe", "Harare", -17.8292, 31.0522, 390757,
     "africa/zimbabwe-latest.osm.pbf", None,
     (25.2194, -22.3973, 33.0428, -15.6148), ""),
]


def falte(text):
    """Vergleichsform eines Namens: ohne Umlaute, ohne Zeichensetzung, klein.
    Damit findet "oesterreich" auch "Oesterreich" mit Umlaut -- wer auf einem
    fremden Geraet tippt, hat die Umlaute vielleicht nicht."""
    text = text.replace("ß", "ss")
    for a, b in (("ä", "ae"), ("ö", "oe"), ("ü", "ue")):
        text = text.replace(a, b)
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    return "".join(c for c in text.lower() if c.isalnum())


class Land(object):
    __slots__ = ("code", "name", "name_en", "ort", "breite", "laenge", "flaeche",
                 "auszug", "steckt_in", "kasten", "hinweis")

    def __init__(self, satz):
        for feld, wert in zip(self.__slots__, satz):
            setattr(self, feld, wert)

    def __repr__(self):
        return "<Land %s %s>" % (self.code, self.name)

    @property
    def schluessel(self):
        """Namensteil fuer Dateien: klein, ohne Zeichen, die ein Dateisystem
        oder eine Befehlszeile anders liest, als sie gemeint sind."""
        return falte(self.name)

    def ueber_datumsgrenze(self):
        return bool(self.kasten) and self.kasten[0] > self.kasten[2]

    def hoehen_kaesten(self):
        """Das Rechteck in ganzen Grad, in der Reihenfolge, die
        `hoehen_holen.py` erwartet: (Sued, West, Nord, Ost). Ueber der
        Datumsgrenze werden es ZWEI -- ein Rechteck von 174 bis -178 Grad
        waere sonst der halbe Erdball."""
        if not self.kasten:
            return []
        w, s, o, n = self.kasten
        s, n = int(math.floor(s)), int(math.ceil(n))
        if self.ueber_datumsgrenze():
            return [(s, int(math.floor(w)), n, 180), (s, -180, n, int(math.ceil(o)))]
        return [(s, int(math.floor(w)), n, int(math.ceil(o)))]


LAENDER = [Land(satz) for satz in TABELLE]

_NACH_SCHLUESSEL = {}
for _l in LAENDER:
    _NACH_SCHLUESSEL[_l.code.lower()] = _l
    for _n in (_l.name, _l.name_en):
        _NACH_SCHLUESSEL.setdefault(falte(_n), _l)


def finde(anfrage):
    """Sucht ueber Gebietscode, deutschen oder englischen Namen. Kein
    Anfangsvergleich: "s" waere sonst die Schweiz, und ein Bau von Stunden
    die Antwort auf einen halb getippten Namen."""
    return _NACH_SCHLUESSEL.get(falte(anfrage or ""))


def alle():
    return list(LAENDER)


def mit_auszug():
    return [l for l in LAENDER if l.auszug]


def ohne_auszug():
    return [l for l in LAENDER if not l.auszug]


def _zeige(l):
    print("%s  %s (%s)" % (l.code, l.name, l.name_en))
    print("  Sprungziel: %s  %.4f %.4f" % (l.ort, l.breite, l.laenge))
    print("  Flaeche:    %s km2" % "{:,}".format(l.flaeche).replace(",", " "))
    if l.kasten:
        rand = "  (ueber die Datumsgrenze)" if l.ueber_datumsgrenze() else ""
        print("  Rechteck:   W %.4f  S %.4f  O %.4f  N %.4f%s"
              % (l.kasten[0], l.kasten[1], l.kasten[2], l.kasten[3], rand))
    if l.auszug:
        print("  Auszug:     %s" % l.auszug)
    elif l.steckt_in:
        print("  KEIN eigener Auszug -- steckt in %s" % l.steckt_in)
    else:
        print("  KEIN Auszug")
    if l.hinweis:
        print("  Hinweis:    %s" % l.hinweis)


def main():
    reste = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--ohne-auszug" in sys.argv:
        for l in ohne_auszug():
            print("%s  %-36s %s" % (l.code, l.name, l.steckt_in or "-- kein Auszug --"))
        return 0
    if reste:
        l = finde(" ".join(reste))
        if not l:
            print("Nicht gefunden: %s" % " ".join(reste))
            return 1
        _zeige(l)
        return 0
    eigen = len(mit_auszug())
    sammel = len([l for l in LAENDER if not l.auszug and l.steckt_in])
    print("Gebiete gesamt:            %d" % len(LAENDER))
    print("mit eigenem Auszug:        %d" % eigen)
    print("nur in einem Sammelpaket:  %d" % sammel)
    print("ohne jeden Auszug:         %d" % (len(LAENDER) - eigen - sammel))
    print("ueber die Datumsgrenze:    %s"
          % ", ".join(l.code for l in LAENDER if l.ueber_datumsgrenze()))
    print("Flaeche aller Gebiete:     %s km2"
          % "{:,}".format(int(sum(l.flaeche or 0 for l in LAENDER))).replace(",", " "))
    return 0


if __name__ == "__main__":
    sys.exit(main())
