package pt.leonida.atlas

import android.app.Application
import pt.leonida.atlas.data.*

class AtlasApp: Application() {
 val repository by lazy { createAtlasRepository(this) }
}
