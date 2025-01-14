package com.dxc.ssi.agent.didcomm.model.other

import com.dxc.ssi.agent.didcomm.model.envelop.EncryptedEnvelop
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
* {
  "to": "5NHSi19Gt4CgJYkbPNrn7MghpZKwLiQM8Kx6g82J8HbY",
  "msg": {
    "protected": "eyJlbmMiOiJ4Y2hhY2hhMjBwb2x5MTMwNV9pZXRmIiwidHlwIjoiSldNLzEuMCIsImFsZyI6IkF1dGhjcnlwdCIsInJlY2lwaWVudHMiOlt7ImVuY3J5cHRlZF9rZXkiOiI3UTRYTG9IdVdsSXVpdVZJc2hTYUp1dUNiRXVad0w1Q1VjckxzV0pXUm5Wd0NpREMxTUozT19fVjJidkZfZkhuIiwiaGVhZGVyIjp7ImtpZCI6IjVOSFNpMTlHdDRDZ0pZa2JQTnJuN01naHBaS3dMaVFNOEt4Nmc4Mko4SGJZIiwiaXYiOiJCaFhSU09heEZ5VjZKN3VtS21XSzBuWHdYWmpwd2dIQyIsInNlbmRlciI6IkZqOTJpSFlrUFlES3F4eGVMWTFXd0tEN19VYVhxQkJQSW9tYnExS0ZuUnNjVTBkX1RZanBYbHk3QjBfUDZ4M3dMb0dhcnFFRnlrc29VQVRlLTE4Mm5KNzE0aE83X2UzQ0dMTjV3T3haRXVOeWVUWEtidEE5eURPVndSTT0ifX1dfQ==",
    "iv": "Erjmpk6urJjijL47",
    "ciphertext": "6ig8uEVwPv1ZIhlX0eexw6F4OBF2wcO-lUlWEmM2xE_7VSrBIBEmFnlcFdTXInCCDvZSXU04bj3eICbIHsK0c22nacWCsoFOfSaO7GgcO8934mJxDXf1WktjwfwsNok1NwsZ34rybHxEdgjQcU3AGDfFOXsLH3j_uXd67lj4oCqJRr9FQG28qnQ1mhBNk_a9qE-jQ3_wxEtuKqXA5gkPhhd7pFk_vtRMb_qrHuZVVMdBMiGdpZoYiLUHt9xTRCpuXweUa8ZfOIIyHmfIU60i8nVbjvMkFEbXsJD0RsIcLNP5zmQex9nxbRpj4eyjm94sHruQ0AUqN5jhAOipJyURncYdjwWk7I90voJtfO6h6dZDrDQH01E22bpoLsna8rBTtvrnpk3korDDCjx4AeizGH3w23S5S70K3bTkQ-qW9N_DpTgPKTfqDzyxS5nKI-l8IfpXlgYnQEWQ7oX03rxGVOk7Bq0J5PphUUkioEGWb4bhkZsYVk6MiWn6_DpzwfuFQP2xdftdyXzv1uNol1eP96uljktIpJPb25odj74lpNmjHDbQ_zJqaB3oRg4OtHju9554W3LcjTxr7iJ1FPs22hucyZ9mLDsOmQnAXjrEjN_e2XC8HqyDoHbet744sCSWqnio-kX8pgLJrVNBrC5eZsi2jpHUUfFn1n-U9Eb8eIlZj3w1T2fQtKnGlH6c0H3ZoVgWEQjqDSE0d53ByU6YAuXfABjIhuGnbz5DnQq2KQZvEwRkjuhBs-oyc_ahr55MQxDGibBwoHiu7dh2C-45QJFI1Yijyb4KQe8jOu2ZFJg38KK8sVBLO_lV50MJWdSb-cyI2nLKU-LZAk82gXySqcONac8ABPQ9UFOUQlKx8K_wj-uXNr2TKaumMONNjHYudSAzcZLwRv4oB7aQDkyktOPkrTHAnPP4aBEURFo=",
    "tag": "smiLW5KFTNiz1Up1SNi81A=="
  },
  "@id": "4d92002e-a802-437c-922c-1918bff7e9aa",
  "@type": "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/routing/1.0/forward"
}
* */

//TODO: create tests for this class
//TODO: think about some model types instead of just strings
//TODO: think if forward message should belong to didexchange package or it is some other feature
@Serializable
data class Forward(
    @Required @SerialName("@type") val type: String = "https://didcomm.org/routing/1.0/forward",
    @SerialName("@id") val id :String,
    val to: String,
    val msg: EncryptedEnvelop
)