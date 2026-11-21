from fastapi import FastAPI, HTTPException

app = FastAPI(title="StudyFlow")
proyectos = [{"id": 1, "nombre": "Exposición de arquitectura"}]
tareas = [{"id": 1, "proyecto": 1, "titulo": "Investigar patrones", "responsable": "Ángel", "participacion": 1}]
datos = {"proyectos": proyectos, "tareas": tareas, "integrantes": [{"id": 1, "nombre": "Ángel"}], "participaciones": [{"id": 1, "proyecto": 1, "integrante": 1}], "recursos": []}

@app.get("/api/panel")
def panel():
    return {"proyectos": proyectos, "tareas": tareas}

from .integraciones import buscar_libros, resumen

app.get("/api/libros")(buscar_libros)
app.get("/api/resumen")(resumen)

@app.get("/api/{coleccion}")
def listar(coleccion: str):
    if coleccion not in datos:
        raise HTTPException(404, "Colección desconocida")
    return datos[coleccion]
