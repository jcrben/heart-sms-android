package xyz.heart.sms.fragment.bottom_sheet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContextWrapper
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.IllegalFormatConversionException

import xyz.heart.sms.R
import xyz.heart.sms.shared.data.DataSource
import xyz.heart.sms.shared.data.model.ScheduledMessage
import xyz.heart.sms.fragment.ScheduledMessagesFragment
import xyz.heart.sms.shared.service.jobs.ScheduledMessageJob
import xyz.heart.sms.shared.util.TimeUtils

@Suppress("DEPRECATION")
class EditScheduledMessageFragment : TabletOptimizedBottomSheetDialogFragment() {

    private var fragment: ScheduledMessagesFragment? = null
    private var scheduledMessage: ScheduledMessage? = null

    private lateinit var format: DateFormat

    private lateinit var sendDate: TextView
    private lateinit var messageText: EditText
    private lateinit var repeat: Spinner

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private val contextToFixDatePickerCrash: ContextWrapper
        get() = object : ContextWrapper(activity) {

            private var wrappedResources: Resources? = null

            override fun getResources(): Resources {
                val r = super.getResources()
                if (wrappedResources == null) {
                    wrappedResources = object : Resources(r.assets, r.displayMetrics, r.configuration) {
                        @Throws(Resources.NotFoundException::class)
                        override fun getString(id: Int, vararg formatArgs: Any): String {
                            return try {
                                super.getString(id, *formatArgs)
                            } catch (ifce: IllegalFormatConversionException) {
                                Log.e("DatePickerDialogFix", "IllegalFormatConversionException Fixed!", ifce)
                                var template = super.getString(id)
                                template = template.replace(("%" + ifce.conversion).toRegex(), "%s")
                                String.format(configuration.locale, template, *formatArgs)
                            }

                        }
                    }
                }

                return wrappedResources!!
            }
        }

    override fun createLayout(inflater: LayoutInflater): View {
        val contentView = inflater.inflate(R.layout.bottom_sheet_edit_scheduled_message, null, false)

        format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)

        sendDate = contentView.findViewById<TextView>(R.id.send_time)
        messageText = contentView.findViewById<EditText>(R.id.message)
        repeat = contentView.findViewById<Spinner>(R.id.repeat_interval)
        val name = contentView.findViewById<TextView>(R.id.contact_name)
        val delete = contentView.findViewById<Button>(R.id.delete)
        val save = contentView.findViewById<Button>(R.id.save)
        val send = contentView.findViewById<Button>(R.id.send)

        if (scheduledMessage != null) {
            messageText.setText(scheduledMessage!!.data)
            sendDate.text = format.format(scheduledMessage!!.timestamp)
            name.text = scheduledMessage!!.title
            messageText.setSelection(messageText.text.length)

            repeat.adapter = ArrayAdapter.createFromResource(activity!!, R.array.scheduled_message_repeat, android.R.layout.simple_spinner_dropdown_item)
            repeat.setSelection(scheduledMessage!!.repeat)
        }

        save.setOnClickListener { save() }
        delete.setOnClickListener { delete() }
        send.setOnClickListener { send() }
        sendDate.setOnClickListener { displayDateDialog() }

        return contentView
    }

    fun setMessage(message: ScheduledMessage) {
        this.scheduledMessage = message
    }

    fun setFragment(fragment: ScheduledMessagesFragment) {
        this.fragment = fragment
    }

    private fun save() {
        val activity = activity ?: return

        if (scheduledMessage == null) {
            return
        }

        scheduledMessage!!.data = messageText.text.toString()
        scheduledMessage!!.repeat = repeat.selectedItemPosition
        DataSource.updateScheduledMessage(activity, scheduledMessage!!)

        dismiss()
        fragment?.loadMessages()
    }

    private fun delete() {
        val activity = activity ?: return

        DataSource.deleteScheduledMessage(activity, scheduledMessage!!.id)

        dismiss()
        fragment?.loadMessages()
    }

    private fun send() {
        scheduledMessage!!.timestamp = TimeUtils.now
        save()

        ScheduledMessageJob.scheduleNextRun(fragment!!.activity!!)
    }

    private fun displayDateDialog() {
        val context = contextToFixDatePickerCrash

        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            scheduledMessage!!.timestamp = GregorianCalendar(year, month, day)
                    .timeInMillis
            displayTimeDialog()
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show()
    }

    private fun displayTimeDialog() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(activity, { _, hourOfDay, minute ->
            scheduledMessage!!.timestamp += (1000 * 60 * 60 * hourOfDay).toLong()
            scheduledMessage!!.timestamp += (1000 * 60 * minute).toLong()

            if (scheduledMessage!!.timestamp < TimeUtils.now) {
                Toast.makeText(activity, R.string.scheduled_message_in_future,
                        Toast.LENGTH_SHORT).show()
                displayDateDialog()
            } else {
                sendDate.text = format.format(scheduledMessage!!.timestamp)
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(activity))
                .show()
    }
}