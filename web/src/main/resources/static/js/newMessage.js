document.addEventListener('DOMContentLoaded', function () {
    $.validator.setDefaults({
        highlight: function (element) {
            $(element)
                .closest('.form-group')
                .addClass('has-warning');
        },
        unhighlight: function (element) {
            $(element)
                .closest('.form-group')
                .removeClass('has-warning');
        }
    });

    $('#sendMessageForm').validate({
        rules: {
            to: {
                required: true,
                remote: {
                    url: '/api/v1.0/users/check/username',
                    type: "GET",
                    data: {
                        username: function () {
                            return $('#to').val();
                        }
                    }
                }
            },
            subject: {
                required: true,
                minlength: 1,
                maxlength: 255
            },
            text: {
                required: true,
                minlength: 1,
                maxlength: 4000
            }
        },
        messages: {
            to: {
                remote: $.validator.format('User {0} doesn`t exist')
            }
        },
        submitHandler: function() { sendMessage() }
    });

    function sendMessage() {
        var sendMessageDTO = { "to":$('#to').val(),
                               "subject":$('#subject').val(),
                               "text":$('#text').val()};
        $.ajax({
            type: 'POST',
            url: '/api/v1.0/messages',
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            data: JSON.stringify(sendMessageDTO),
            complete: function (event) {
                if(event.status === 201) {
                    $('#to').val('');
                    $('#subject').val('');
                    $('#text').val('');

                    $('#sendMessageForm')
                        .before('<div class="alert alert-dismissable alert-success">'
                              + '<span th:text="#{newMessage.success}">The message was sent!</span>'
                              + '</div>');

                    setTimeout('hiddenAlertAfterSeconds()', 2000);
                } else {
                    if(event.status === 403 || event.status === 409) {
                        $('#to')
                            .after('<label id="to-error" class="error" for="to">' + 'You cannot send a message to yourself.' + '</label>');

                        return;
                    }
                    var obj = JSON.parse(event.responseText);
                    for(var i = 0; i < obj.field_errors.length; ++i) {
                        var objError = obj.field_errors[i];

                        $('#' + objError.field)
                            .after('<label id="' + objError.field + '-error" class="error" for="' + objError.field + '">' + objError.message + '</label>');
                    }
                }
            }
        });
    }
});

function hiddenAlertAfterSeconds() {
    $('.alert').remove();
}
