package ru.jr2.edit.presentation.sentence.view

import javafx.geometry.Pos
import javafx.scene.layout.Priority
import ru.jr2.edit.Style
import ru.jr2.edit.presentation.sentence.model.SentenceModel
import ru.jr2.edit.presentation.base.view.BaseEditFragment
import ru.jr2.edit.presentation.sentence.viewmodel.SentenceEditViewModel
import tornadofx.*

class SentenceEditFragment : BaseEditFragment<SentenceModel, SentenceEditViewModel>() {
    override val viewModel = SentenceEditViewModel(paramItemId)

    override val root = borderpane {
        center = form {
            fieldset {
                field("Предложение") {
                    textarea(viewModel.pSentence) {
                        vgrow = Priority.NEVER
                        required()
                    }
                }
                field("Фуригана") {
                    textarea(viewModel.pFurigana) {
                        vgrow = Priority.NEVER
                    }
                }
                field("Интерпретация") {
                    textarea(viewModel.pInterpretation) {
                        vgrow = Priority.NEVER
                    }
                }
            }
        }

        bottom = hbox {
            button("Сохранить") {
                enableWhen(viewModel.valid)
                action {
                    viewModel.onSaveClick()
                    close()
                }
                addClass(Style.largeButton)
            }
            alignment = Pos.BOTTOM_RIGHT
        }
        paddingAll = 10.0
    }
}