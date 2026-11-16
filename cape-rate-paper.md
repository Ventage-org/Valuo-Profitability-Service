Third Contribution: Profitability and Cap Rate Financial Analysis
Valuo integrates an interactive financial simulator that calculates the property's operating return, on demand, as an investment alternative. The module links the projected sale price and rental income to compute the Net Operating Income (NOI), discounting a standard 20% Operating Expense Ratio (OER). It then derives the annualized Cap Rate and gross return, providing advisors with objective metrics to guide purchase and investment decisions.
 pantallla coin lo que estamos trabajando en el backend
Fig. 3.	Interactive profitability and Cap Rate analysis screen

.... mas infomracion en el paper

V.	EXPERIMENTS
Procedimiento y Construcción del Conjunto de Datos
La base analítica se construyó a partir de los registros históricos de compraventa y alquiler residencial del BCRP y el portal Urbania (2016–2025). Se descartaron los distritos con una sola transacción (n=1) en diez años por carecer de varianza estadística, consolidando un universo estrictamente homólogo de 22 distritos con mercado formal activo (más del 99.99% de la muestra original) para garantizar coherencia al cruzar ambas predicciones en el cálculo del Cap Rate y PER. Tras depurar valores atípicos e imputar atributos físicos faltantes mediante la mediana y moda distrital, el conjunto limpio quedó integrado por 67,910 transacciones de venta y 61,603 contratos de alquiler.

....mas texto y mas informacion....

Partición Temporal Cero-Fuga y Formulación del Modelo
Para evitar la fuga de información temporal (look-ahead bias), se aplicó una partición cronológica estricta (Zero-Leakage Out-of-Time): el conjunto de entrenamiento (2016–2023) agrupa 44,173 registros de venta y 41,921 de alquiler, mientras que el conjunto de prueba (2024–2025) reserva a ciegas 23,737 registros de venta y 19,682 de alquiler, sin participar en la validación temprana del gradiente. Para priorizar el régimen de mercado pospandemia sin descartar el historial, se ponderó la función de pérdida con un decaimiento exponencial interanual w_i=〖0.85〗^((2023-〖"Año" 〗_i ) ). Los montos nominales fueron deflactados a Soles Constantes (IPC Base Dic. 2009 = 100) y, siguiendo las directrices de la IAAO para mitigar la heterocedasticidad entre inmuebles de distinta escala, ambos regresores XGBoost (entrenados mediante histogramas y regularización L_1/L_2 sobre las 33 variables simétricas) predicen el logaritmo natural del valor unitario por metro cuadrado (y_i), reconstruyendo la estimación monetaria (P ̂_(i,d)) mediante un factor de calibración distrital (〖"Factor" 〗_d):
y_i=ln⁡(〖"Valor_Soles_Const" 〗_i/〖"Superficie" 〗_i ), P ˆ_(i,d)=(exp⁡(y ˆ_i )/〖"Factor" 〗_d )×〖"Superficie" 〗_i
Tanto la codificación espacial distrito_encoded (Target Encoding Bayesiano con m=10.0) como el factor de calibración post-hoc (〖"Factor" 〗_d) se aprenden exclusivamente en el conjunto de entrenamiento (ventana 2023). Con ambas inferencias en escala monetaria real, el sistema calcula el PER, el Cap Rate Bruto y el Cap Rate Neto, descontando un Ratio de Gastos Operativos ("OER" ) base del 15% (configurable por el usuario en la interfaz):

"Cap Rate Neto (%)"=((P ˆ_"alq" ×12)×(1-"OER" ))/P ˆ_"venta"  ×100

Desempeño Predictivo Econométrico (Modelos de Venta y Alquiler)
La evaluación sobre la partición ciega de prueba (Test Set 2024–2025: 23,737 transacciones de venta y 19,682 contratos de alquiler) confirma la capacidad de generalización de ambos algoritmos hacia escenarios futuros sin fuga de datos. El modelo xgboost_venta alcanzó un MAPE de 14.86% y un coeficiente de determinación R^2 de 0.7477, registrando un MAE de S/. 51,861.76 y un RMSE de S/. 78,053.38 en escala monetaria real. De forma paralela, el modelo xgboost_alquiler obtuvo un MAPE de 13.68% y un R^2 de 0.6761, con un margen de error absoluto medio (MAE) de S/. 235.28/mes y un RMSE de S/. 361.10/mes. Ambos estimadores se sitúan por debajo del umbral crítico internacional ("MAPE"≤15.0%), demostrando que la formulación logarítmica unitaria (ln⁡(P/m^2 )) y la simetría de las 33 variables predictivas eliminan distorsiones de escala entre tipologías de inmuebles y proveen insumos financieramente consistentes para derivar el Cap Rate Neto (con OER base del 15%) y el ratio PER.


