Плагин главного табло. В основе дизайна HTML.

Остальные файлы в папку config сервера.

Привязка к рабочим местам

id - Абстрактный идентификатор рабочего места. Привязывается к номеру пункта приема посетителей в bs.adr. Пункт приема посетителей это атрибут пользователя "Идентификатор"
{{id|blink}}  пометить миганием. Это значение параметра class для тега в html
{{id|service}} text название услуги вызванного посетителя
{{id|discription}} text описание услуги  вызванного посетителя
{{id|user}} text название пользователя  вызвавшего посетителя
{{id|ext}} text дополнительная колонка пользователя  вызвавшего посетителя
{{id|point}} text атрибут пользователя "Идентификатор"
{{id|queueXX}} номерок посетителя. XX позиция в очереди к оператору id


Привязка к строкам вызова

XX - номер строки
{{blinkXX}} - Это значение параметра class для тега в html. Строки могут мигать при вызове.
{{ticketXX}} - будет заменено значением номера тилона в строке XX
{{pointXX}} - будет заменено значением идентификатора пункта приема в строке XX
{{extXX}} - будет заменено значением расширенного стролбца для юзера в строке XX

Пример:
Первая и вторая строка табло вызова в случае верстки на таблицах
<tr>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink1}}">{{ticket1}}</span></td>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink1}}">{{point1}}</span></td>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink1}}">{{ext1}}</span></td>
</tr>
<tr>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink2}}">{{ticket2}}</span></td>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink2}}">{{point2}}</span></td>
<td width="50%" align="center"><span style='font-size:30.0pt;color:yellow' id="{{blink2}}">{{ext2}}</span></td>
</tr>



Привязка списка ближайших
{{nextXX}}, где XX - инкрементальный счетчик, например {{next1}} {{next2}} ... {{next85}} ... . Добавить столько сколько ближайших нужно показывать.