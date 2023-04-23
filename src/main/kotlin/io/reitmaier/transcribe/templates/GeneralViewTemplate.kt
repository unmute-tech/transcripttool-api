package io.reitmaier.transcribe.templates

import io.ktor.server.html.*
import kotlinx.datetime.*
import kotlinx.html.*
import io.reitmaier.transcribe.db.Deployment
import io.reitmaier.transcribe.db.Hydrated_task
import io.reitmaier.transcribe.db.User
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StatusViewTemplate(
  private val deployment: Deployment,
  private val users: List<User>,
  private val tasks: List<Hydrated_task>,
  private val now: Instant = Clock.System.now(),
): Template<FlowContent> {
  override fun FlowContent.apply() {
    h1(classes = "title") { +"Human Transcription Deployment Status"}
    insert(UserStatus(users, tasks)){}
    insert(TaskStatus(tasks, users)){}
  }
}


class TaskRow(
  private val task: Hydrated_task,
  private val audio: Boolean
) : Template<TR> {
  override fun TR.apply() {
    th {
      +"${task.path.substringAfter("/")}"
      if(audio) {
        br {}
        audio {
          controls = true
          src = "/user/${task.user_id.value}/task/${task.id.value}/file"
          attributes["type"] = "audio/mpeg"
          attributes["preload"] = "none"
        }
      }
    }
    td { +"${task.user_id.value} "}
      td {
        if(task.transcript.isNullOrBlank() && task.reject_reason == null) {
          em {
            +"Awaiting Transcription"
          }
        } else if(task.reject_reason != null) {
          em {
            +"${task.reject_reason}"
          }
        }
        else {
          +"${task.transcript}"
        }
      }
  }
}
class TaskStatus(
  private val tasks: List<Hydrated_task>,
  private val users: List<User>,
) : Template<FlowContent> {
  override fun FlowContent.apply() {
    val groupedTasks = tasks.groupBy { it.path }.map { it.key to it.value }
      .sortedByDescending { it.second.size  }
    h2(classes = "title") { +"Tasks"}
    table(
      classes = "table is-bordered is-hoverable"
    ) {
      // TODO headers
      // TODO footers
      thead {
        tr {
          th { +"ID"}
          th { +"User"}
          th { +"Transcript"}
        }
      }
      tbody {
        for (t in groupedTasks) {
          t.second.forEachIndexed { index, item ->
            tr {
              insert(TaskRow(item, index == 0)) {}
            }
          }
        }
      }
    }
  }
}

class UserStatus(
  private val users: List<User>,
  private val tasks: List<Hydrated_task>,
) : Template<FlowContent> {
  override fun FlowContent.apply() {
    h2(classes = "title") { +"Users"}
    table(
      classes = "table is-bordered is-hoverable"
    ) {
      // TODO headers
      // TODO footers
      thead {
        tr {
          th { +"User"}
          th { +"Assignments"}
          th { +"Incomplete"}
          th { +"Completed"}
          th { +"Earnings (R20 pm)"}
        }
      }
      tbody {
        for (s in users) {
          val userTask = tasks.filter { it.user_id == s.id }
          val completed = userTask.count { it.completed_at != null}
          val incomplete = userTask.count { it.completed_at == null}
          val length = userTask.sumOf { it.length }.milliseconds
          tr {
            insert(UserRow(s, completed, incomplete, length)) {}
          }
        }
      }
    }
  }
}

class UserRow(
  private val user: User,
  private val completed: Int,
  private val incomplete: Int,
  private val length: Duration,
) : Template<TR> {
  override fun TR.apply() {
    th { +"${user.name.value} (${user.id.value})"}
    td { +"${completed + incomplete} "}
    td { +"$incomplete"}
    td { +"$completed (${length.inWholeMinutes}m)"}
    td { +"R${(length.inWholeMinutes + 1) * 20}"}
  }
}



class Layout: Template<HTML> {
  val content =  Placeholder<HtmlBlockTag>()
//  val menu = TemplatePlaceholder<NavTemplate>()

  override fun HTML.apply() {
    head{
      title { +"Transcript Status" }
      meta { charset = "UTF-8" }
      meta {
        name = "viewport"
        content = "width=device-width, initial-scale=1"
      }

      link(
        rel = "stylesheet",
        href = "/static/css/status.css",
        type = "text/css"
      )

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.9.3/css/bulma.min.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-IgmDkwzs96t4SrChW29No3NXBIBv8baW490zk5aXvhCD8vuZM3yUSkbyTBcXohkySecyzIrUwiF/qV0cuPcL3Q=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/bulma-tooltip/1.2.0/bulma-tooltip.min.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-eQONsEIU2JzPniggWsgCyYoASC8x8nS0w6+e5LQZbdvWzDUVfUh+vQZFmB2Ykj5uqGDIsY7tSUCdTxImWBShYg=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-xodZBNTC5n17Xt2atTPuE1HxjVMSvLVW9ocqUKLsCC5CXdbqCmblAshOMAS6/keqq/sMZMZ19scR4PsZChSR7A=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      script(
        src = "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.min.js"
      ) {
        this.integrity = "sha512-SeiQaaDh73yrb56sTW/RgVdi/mMqNeM2oBwubFHagc5BkixSpP1fvqF47mKzPGWYSSy4RwbBunrJBQ4Co8fRWA=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      script(
        src = "https://cdnjs.cloudflare.com/ajax/libs/leaflet-providers/1.13.0/leaflet-providers.min.js"
      ) {
        this.integrity = "sha512-5EYsvqNbFZ8HX60keFbe56Wr0Mq5J1RrA0KdVcfGDhnjnzIRsDrT/S3cxdzpVN2NGxAB9omgqnlh4/06TvWCMw=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }
    }

    body{
      section(classes = "section") {
        div(classes = "container") {
          div(classes = "content") {
            insert(content)
          }
        }
      }

    }
  }

}
