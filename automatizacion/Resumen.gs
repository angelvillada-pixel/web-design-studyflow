function actualizarResumen() {
  const libro = SpreadsheetApp.getActiveSpreadsheet();
  const tareas = libro.getSheetByName('Tareas');
  if (!tareas) throw new Error('Crea Tareas con columnas Proyecto, Título, Estado');
  const filas = tareas.getDataRange().getValues().slice(1);
  const pendientes = filas.filter(f => f[0] && f[2] !== 'Completada');
  const salida = libro.getSheetByName('Resumen') || libro.insertSheet('Resumen');
  salida.clearContents();
  const valores = [['Proyecto', 'Título'], ...pendientes.map(f => f.slice(0, 2))];
  salida.getRange(1, 1, valores.length, 2).setValues(valores);
}