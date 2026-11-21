from os import getenv
from urllib.parse import urlencode
from urllib.request import urlopen, Request
import json

from fastapi import HTTPException, Query


def buscar_libros(consulta: str = Query(min_length=2, max_length=100)):
    params = urlencode({"q": consulta, "fields": "key,title,author_name", "limit": 5})
    request = Request("https://openlibrary.org/search.json?" + params,
                      headers={"User-Agent": "StudyFlowAcademic/1.0"})
    try:
        with urlopen(request, timeout=6) as response:
            docs = json.load(response)["docs"]
    except (OSError, ValueError, KeyError) as error:
        raise HTTPException(503, "Bibliografía temporalmente no disponible") from error
    return [doc["title"] for doc in docs if doc.get("title")]


def resumen():
    hoja = getenv("GOOGLE_SHEETS_ID")
    if not hoja:
        raise HTTPException(503, "Falta configurar la hoja")
    import google.auth
    from google.auth.transport.requests import AuthorizedSession
    from google.auth.exceptions import GoogleAuthError
    from requests.exceptions import RequestException
    try:
        credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/spreadsheets.readonly"])
        with AuthorizedSession(credentials) as session:
            respuesta = session.get(f"https://sheets.googleapis.com/v4/spreadsheets/{hoja}/values/Resumen!A2:B", timeout=6)
            respuesta.raise_for_status()
            return [" · ".join(map(str, fila)) for fila in respuesta.json().get("values", [])]
    except (GoogleAuthError, RequestException) as error:
        raise HTTPException(503, "No se pudo leer el resumen de Sheets") from error
